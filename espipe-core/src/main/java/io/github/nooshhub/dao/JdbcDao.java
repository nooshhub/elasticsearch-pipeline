/*
 * Copyright 2021-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.nooshhub.dao;

import java.sql.JDBCType;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.zaxxer.hikari.HikariDataSource;
import io.github.nooshhub.common.metric.JdbcMetric;
import io.github.nooshhub.config.EspipeElasticsearchProperties;
import io.github.nooshhub.config.IndexConfig;
import io.github.nooshhub.config.IndexConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * Jdbc DAO, poll data from database by JDBC.
 *
 * @author Neal Shan
 * @since 5/31/2022
 */
@Service
public class JdbcDao {

    private static final Logger logger = LoggerFactory.getLogger(JdbcDao.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ElasticsearchDao elasticsearchDao;

    @Autowired
    private EspipeTimerDao espipeTimerDao;

    @Autowired
    private EspipeElasticsearchProperties espipeElasticsearchProperties;

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    public JdbcMetric jdbcMetrics() {
        JdbcMetric metric = new JdbcMetric();
        metric.setFetchSize(this.jdbcTemplate.getFetchSize());
        metric.setMaxRows(this.jdbcTemplate.getMaxRows());
        metric.setQueryTimeout(this.jdbcTemplate.getQueryTimeout());

        // connection size and status
        HikariDataSource ds = (HikariDataSource) this.jdbcTemplate.getDataSource();
        if (ds != null) {
            metric.setMaxPoolSize(ds.getMaximumPoolSize());
            metric.setActiveConnections(ds.getHikariPoolMXBean().getActiveConnections());
        }

        return metric;
    }

    /**
     * init data to index.
     * @param indexName index name
     */
    public void init(String indexName) {

        if (!this.elasticsearchDao.isServerUp()) {
            logger.error("Elasticsearch server is not accessible, please Check.");
            return;
        }

        jdbcMetrics();

        this.espipeTimerDao.delete(indexName);
        this.elasticsearchDao.createIndex(indexName);

        StopWatch sw = new StopWatch();
        sw.start();

        IndexConfig indexConfig = this.indexConfigRegistry.getIndexConfig(indexName);
        LocalDateTime currentRefreshTime = LocalDateTime.now(ZoneId.systemDefault());
        List<CompletableFuture<BulkResponse>> futures = new ArrayList<>();
        List<Map<String, Object>> flattenMapList = new ArrayList<>(this.jdbcTemplate.getFetchSize());
        this.jdbcTemplate.query(indexConfig.getInitSql(), new Object[] { currentRefreshTime },
                new int[] { JDBCType.TIMESTAMP.getVendorTypeNumber() }, (rs) -> {
                    Map<String, Object> flattenMap = createStandardFlattenMap(rs);
                    flattenMapList.add(flattenMap);

                    // send per jdbcTemplate.getFetchSize()
                    boolean isSend = (rs.getRow() % this.espipeElasticsearchProperties.getBulkSize() == 0);
                    if (isSend) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("index data size {}", flattenMapList.size());
                        }
                        extendFlattenMap(indexName, flattenMapList);
                        futures.add(this.elasticsearchDao.createDocument(indexName, flattenMapList));
                        flattenMapList.clear();
                    }

                });

        // process the rest of data, like we have total 12038, the above will process
        // 12000, the rest 38 will be processed here
        if (!flattenMapList.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("index data size {}", flattenMapList.size());
            }
            extendFlattenMap(indexName, flattenMapList);
            futures.add(this.elasticsearchDao.createDocument(indexName, flattenMapList));
            flattenMapList.clear();
        }

        this.elasticsearchDao.processCompletableFutures(indexName, futures);

        this.elasticsearchDao.updateSettingsAfterInit(indexName);
        sw.stop();

        // reset after init script finished, or sync script will create document, index
        // settings will be changed, since sync is based on last refresh time.
        this.espipeTimerDao.save(indexName, currentRefreshTime);
        logger.info("Init index {} success", indexName);
        logger.info("Total time: {}s", sw.getTotalTimeSeconds());
    }

    /**
     * Init one index with ids and values in a map.
     *
     * @param indexName index name
     * @param idAndValueMap id and value map
     */
    public void init(String indexName, Map<String, String> idAndValueMap) {
        if (!this.elasticsearchDao.isServerUp()) {
            logger.error("Elasticsearch server is not accessible, please Check.");
            return;
        }

        StopWatch sw = new StopWatch();
        sw.start();

        IndexConfig indexConfig = this.indexConfigRegistry.getIndexConfig(indexName);
        LocalDateTime currentRefreshTime = LocalDateTime.now(ZoneId.systemDefault());
        List<CompletableFuture<BulkResponse>> futures = new ArrayList<>();
        List<Map<String, Object>> flattenMapList = new ArrayList<>(this.jdbcTemplate.getFetchSize());

        // prepare the sql for init one index
        StringBuilder sql = new StringBuilder();
        sql.append(indexConfig.getInitSql());
        idAndValueMap.forEach((id, value) -> {
            sql.append(" AND ").append(id).append(" = ?");
        });

        this.jdbcTemplate.query((conn) -> {
                    final PreparedStatement ps = conn.prepareStatement(sql.toString());
                    int index = 1;
                    ps.setTimestamp(index, Timestamp.valueOf(currentRefreshTime));
                    idAndValueMap.forEach((id, value) -> {
                        try {
                            ps.setLong(index + 1, Long.parseLong(value));
                        } catch (SQLException ex) {
                            throw new IllegalArgumentException(String.format("value %s is incorrect, %s", value, ex.getMessage()));
                        }
                    });
                    return ps;
                },
                (rs) -> {
                    Map<String, Object> flattenMap = createStandardFlattenMap(rs);
                    flattenMapList.add(flattenMap);

                    if (logger.isDebugEnabled()) {
                        logger.debug("index data size {}", flattenMapList.size());
                    }
                    extendFlattenMap(indexName, flattenMapList);
                    futures.add(this.elasticsearchDao.createDocument(indexName, flattenMapList));
                    flattenMapList.clear();
                });

        if (futures.size() == 0) {
            // TODO: the exception from a child thread does not seem to be caught.
            throw new IllegalArgumentException(String.format("id %s is not exist", Arrays.toString(idAndValueMap.values().toArray())));
        } else {
            this.elasticsearchDao.processCompletableFutures(indexName, futures);

            sw.stop();

            logger.info("Init one index {} success", indexName);
            logger.info("Total time: {}s", sw.getTotalTimeSeconds());
        }

    }

    /**
     * sync data to index.
     * @param indexName index name
     */
    public void sync(String indexName) {
        if (!this.elasticsearchDao.isIndexExist(indexName)) {
            logger.error("Index {} not exist, please init index manually.", indexName);
            return;
        }

        // get the last refresh time from the database, to continue synchronizing
        LocalDateTime lastRefreshTime = this.espipeTimerDao.findLastRefreshTime(indexName);
        if (lastRefreshTime == null) {
            logger.warn("LastRefreshTime is null, please init index {} manually.", indexName);
            return;
        }

        LocalDateTime currentRefreshTime = LocalDateTime.now(ZoneId.systemDefault());

        this.espipeTimerDao.save(indexName, currentRefreshTime);

        final String syncSql = this.indexConfigRegistry.getIndexConfig(indexName).getSyncSql();
        List<Map<String, Object>> flattenMapList = new ArrayList<>(this.jdbcTemplate.getFetchSize());
        this.jdbcTemplate.query((conn) -> {
            LocalDateTime decreasedLastRefreshTime = lastRefreshTime.minusSeconds(1);

            final PreparedStatement ps = conn.prepareStatement(syncSql);
            ParameterMetaData parameterMetaData = ps.getParameterMetaData();
            int paramCount = parameterMetaData.getParameterCount();
            // the param count is depend on the sql, it should be even and is a pair of
            // start and end timestamp.
            for (int i = 0; i < paramCount; i++) {
                if (i % 2 == 0) {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(decreasedLastRefreshTime));
                }
                else {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(currentRefreshTime));
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("syncing data for index {} from {} to {}", indexName,
                        Timestamp.valueOf(decreasedLastRefreshTime), Timestamp.valueOf(currentRefreshTime));
            }

            return ps;
        }, (rs) -> {
            Map<String, Object> flattenMap = createStandardFlattenMap(rs);
            flattenMapList.add(flattenMap);
        });

        if (!flattenMapList.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("syncing data for index {} size {}", indexName, flattenMapList.size());
            }
            extendFlattenMap(indexName, flattenMapList);
            CompletableFuture<BulkResponse> bulkResFuture = this.elasticsearchDao.createDocument(indexName,
                    flattenMapList);

            this.elasticsearchDao.processCompletableFuture(indexName, bulkResFuture);

            flattenMapList.clear();
        }
    }

    /**
     * get total count of table.
     * @param tableName table name
     * @return total count
     */
    public long getTotalCount(String tableName) {
        return this.jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    /**
     * put stand fields in flattenMap.
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
     * put custom fields in flattenMap.
     * @param indexName index name
     * @param flattenMapList flatten Map List
     */
    private void extendFlattenMap(String indexName, List<Map<String, Object>> flattenMapList) {
        IndexConfig indexConfig = this.indexConfigRegistry.getIndexConfig(indexName);
        String extensionColumn = indexConfig.getExtensionColumn();
        String extensionSql = indexConfig.getExtensionSql();

        List<String> extensionIds = new ArrayList<>(100);
        for (Map<String, Object> flattenMap : flattenMapList) {
            if (extensionColumn != null && flattenMap.get(extensionColumn.toLowerCase()) != null) {
                extensionIds.add(String.valueOf(flattenMap.get(extensionColumn.toLowerCase())));
            }
        }

        if (!extensionIds.isEmpty()) {
            extensionSql = extensionSql.replace("?", String.join(",", extensionIds));
            if (FieldsMode.FLATTEN.toString().equals(this.espipeElasticsearchProperties.getFieldsMode())) {

                Map<String, Map<String, Object>> customIdToCustomFlattenMap = new HashMap<>();

                this.jdbcTemplate.query(extensionSql, (prs) -> {
                    ResultSetMetaData prsMetaData = prs.getMetaData();
                    int pcount = prsMetaData.getColumnCount();
                    for (int i = 1; i <= pcount; i++) {
                        Map<String, Object> flattenMap = customIdToCustomFlattenMap.getOrDefault(prs.getString(1),
                                new HashMap<>());
                        flattenMap.put(prs.getString(2).toLowerCase(), prs.getObject(3));
                        customIdToCustomFlattenMap.put(prs.getString(1), flattenMap);
                    }
                });

                for (Map<String, Object> flattenMap : flattenMapList) {
                    if (extensionColumn != null && flattenMap.get(extensionColumn.toLowerCase()) != null) {
                        final Map<String, Object> customFlattenMap = customIdToCustomFlattenMap
                                .get(String.valueOf(flattenMap.get(extensionColumn.toLowerCase())));
                        if (customFlattenMap != null) {
                            flattenMap.putAll(customFlattenMap);
                        }
                    }
                }
            }
            else if (FieldsMode.CUSTOM_IN_ONE.toString().equals(this.espipeElasticsearchProperties.getFieldsMode())) {
                Map<Object, StringBuilder> customIdToSbMap = new HashMap<>();

                this.jdbcTemplate.query(extensionSql, (prs) -> {
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
                        final StringBuilder customSb = customIdToSbMap
                                .get(String.valueOf(flattenMap.get(extensionColumn.toLowerCase())));
                        if (customSb != null) {
                            flattenMap.put("custom_fields", customSb.toString());
                        }
                    }
                }
            }
        }

    }

    enum FieldsMode {

        /**
         * Constant that indicates convert standard fields to a flatten map.
         */
        FLATTEN,

        /**
         * Constant that indicates aggregate standard and custom fields to a string.
         */
        ALL_IN_ONE,

        /**
         * Constant that indicates aggregate custom fields to a string.
         */
        CUSTOM_IN_ONE

    }

}
