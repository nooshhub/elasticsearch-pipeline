/*
 * Copyright 2021-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.github.nooshhub;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Jdbc Pipe, poll data from database by JDBC
 *
 * @author neals
 * @since 5/31/2022
 */
@Service
public class JdbcPipe {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcPipe.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ElasticsearchPipe elasticsearchPipe;
    @Autowired
    private EspipeTimer espipeTimer;
    @Autowired
    private EspipeElasticsearchProperties espipeElasticsearchProperties;

    public void jdbcMetrics() {
        // fetch size
        LOG.info("Fetch size {}, max rows {}, query timeout {}", jdbcTemplate.getFetchSize(), jdbcTemplate.getMaxRows(), jdbcTemplate.getQueryTimeout());
        // connection size and status
        HikariDataSource ds = (HikariDataSource) jdbcTemplate.getDataSource();
        LOG.info("datasource max pool size {}", ds.getMaximumPoolSize());
        // LOG.info("datasource active connections size {}" , ds.getHikariPoolMXBean().getActiveConnections());
    }

    /**
     * init data to index
     *
     * @param indexConfig index config
     */
    public void init(Map<String, String> indexConfig) {

        elasticsearchPipe.createIndex(indexConfig);

        String indexName = indexConfig.get("indexName");
        String initSql = indexConfig.get("initSql");

        LocalDateTime currentRefreshTime = LocalDateTime.now();
        espipeTimer.reset(indexName, currentRefreshTime);

        jdbcMetrics();

        StopWatch sw = new StopWatch();
        sw.start();

        List<Map<String, Object>> flattenMapList = new ArrayList<>(jdbcTemplate.getFetchSize());
        jdbcTemplate.query(initSql,
                new Object[]{currentRefreshTime},
                new int[]{JDBCType.TIMESTAMP.getVendorTypeNumber()},
                rs -> {
                    Map<String, Object> flattenMap = createStandardFlattenMap(rs);
                    flattenMapList.add(flattenMap);

                    // send per jdbcTemplate.getFetchSize()
                    boolean isSend = (rs.getRow() % jdbcTemplate.getFetchSize() == 0);
                    if (isSend) {
                        LOG.info("index data size {}", flattenMapList.size());
                        extendFlattenMap(indexConfig, flattenMapList);
                        elasticsearchPipe.createDocument(indexConfig, flattenMapList);
                        flattenMapList.clear();
                    }

                });

        // process the rest of data, like we have total 108538, the above will process 108500, the rest 38 will be processed here
        if (flattenMapList.size() > 0) {
            LOG.info("index data size {}", flattenMapList.size());
            extendFlattenMap(indexConfig, flattenMapList);
            elasticsearchPipe.createDocument(indexConfig, flattenMapList);
            flattenMapList.clear();
        }

        sw.stop();
        LOG.info("Total time: {}s", sw.getTotalTimeSeconds());
    }

    /**
     * sync data to index
     *
     * @param indexConfig index config
     */
    public void sync(Map<String, String> indexConfig) {
        String indexName = indexConfig.get("indexName");
        String syncSql = indexConfig.get("syncSql");

        // get the last refresh time from the database
        LocalDateTime lastRefreshTime = espipeTimer.findLastRefreshTime(indexName);
        if (lastRefreshTime == null) {
            throw new EspipeException("Please init index " + indexName + " first.");
        }

        LocalDateTime currentRefreshTime = LocalDateTime.now();
        LOG.info("syncing data for index {} from {} to {}", indexName, Timestamp.valueOf(lastRefreshTime), Timestamp.valueOf(currentRefreshTime));

        espipeTimer.reset(indexName, currentRefreshTime);

        List<Map<String, Object>> flattenMapList = new ArrayList<>(jdbcTemplate.getFetchSize());
        jdbcTemplate.query(conn -> {
            final PreparedStatement ps = conn.prepareStatement(syncSql);
            ParameterMetaData parameterMetaData = ps.getParameterMetaData();
            int paramCount = parameterMetaData.getParameterCount();

            // the param count is depend on the sql, it should be even and is a pair of start and end timestamp.
            for (int i = 0; i < paramCount; i++) {
                if (paramCount % 2 == 0) {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(lastRefreshTime));
                } else {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(currentRefreshTime));
                }
            }

            return ps;
        }, rs -> {
            Map<String, Object> flattenMap = createStandardFlattenMap(rs);
            flattenMapList.add(flattenMap);
        });

        if (flattenMapList.size() > 0) {
            LOG.info("syncing data for index {} size {}", indexName, flattenMapList.size());
            extendFlattenMap(indexConfig, flattenMapList);
            elasticsearchPipe.createDocument(indexConfig, flattenMapList);
            flattenMapList.clear();
        }
    }

    /**
     * put stand fields in flattenMap
     *
     * @param rs result set
     * @return flatten map
     * @throws SQLException all sql exception
     */
    private Map<String, Object> createStandardFlattenMap(ResultSet rs) throws SQLException {
        Map<String, Object> flattenMap = new HashMap<>();

        // prepare standard fields
        ResultSetMetaData rsMetaData = rs.getMetaData();
        int count = rsMetaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            flattenMap.put(rsMetaData.getColumnName(i).toLowerCase(), rs.getObject(i));
        }

        return flattenMap;
    }

    /**
     * put custom fields in flattenMap
     *
     * @param indexConfig    index config
     * @param flattenMapList flatten Map List
     */
    private void extendFlattenMap(Map<String, String> indexConfig, List<Map<String, Object>> flattenMapList) {
        String extensionColumn = indexConfig.get("extensionColumn");
        String extensionSql = indexConfig.get("extensionSql");

        List<String> extensionIds = new ArrayList<>(100);
        for (Map<String, Object> flattenMap : flattenMapList) {
            if (extensionColumn != null && flattenMap.get(extensionColumn.toLowerCase()) != null) {
                extensionIds.add(String.valueOf(flattenMap.get(extensionColumn.toLowerCase())));
            }
        }

        if (extensionIds.size() > 0) {
            extensionSql = extensionSql.replace("?", String.join(",", extensionIds));
            if (EspipeFieldsMode.flatten.toString().equals(espipeElasticsearchProperties.getFieldsMode())) {

                Map<String, Map<String, Object>> customIdToCustomFlattenMap = new HashMap<>();

                jdbcTemplate.query(extensionSql,
                        prs -> {
                            ResultSetMetaData prsMetaData = prs.getMetaData();
                            int pcount = prsMetaData.getColumnCount();
                            for (int i = 1; i <= pcount; i++) {
                                Map<String, Object> flattenMap = customIdToCustomFlattenMap.getOrDefault(prs.getString(1), new HashMap<>());
                                flattenMap.put(prs.getString(2).toLowerCase(), prs.getObject(3));
                                customIdToCustomFlattenMap.put(prs.getString(1), flattenMap);
                            }
                        });

                for (Map<String, Object> flattenMap : flattenMapList) {
                    if (extensionColumn != null && flattenMap.get(extensionColumn.toLowerCase()) != null) {
                        final Map<String, Object> customFlattenMap = customIdToCustomFlattenMap.get(String.valueOf(flattenMap.get(extensionColumn.toLowerCase())));
                        if (customFlattenMap != null) {
                            flattenMap.putAll(customFlattenMap);
                        }
                    }
                }
            } else if (EspipeFieldsMode.custom_in_one.toString().equals(espipeElasticsearchProperties.getFieldsMode())) {
                Map<Object, StringBuilder> customIdToSbMap = new HashMap<>();

                jdbcTemplate.query(extensionSql,
                        prs -> {
                            ResultSetMetaData prsMetaData = prs.getMetaData();
                            int pcount = prsMetaData.getColumnCount();
                            for (int i = 1; i <= pcount; i++) {
                                StringBuilder sb = customIdToSbMap.getOrDefault(prs.getString(1), new StringBuilder());
                                sb.append(prs.getString(2).toLowerCase());
                                sb.append(" ");
                                sb.append(prs.getObject(3));
                                sb.append(" ");
                                customIdToSbMap.put(prs.getString(1), sb);
                            }
                        });

                for (Map<String, Object> flattenMap : flattenMapList) {
                    if (extensionColumn != null && flattenMap.get(extensionColumn.toLowerCase()) != null) {
                        final StringBuilder customSb = customIdToSbMap.get(String.valueOf(flattenMap.get(extensionColumn.toLowerCase())));
                        if (customSb != null) {
                            flattenMap.put("custom_fields", customSb.toString());
                        }
                    }
                }
            }
        }

    }

}
