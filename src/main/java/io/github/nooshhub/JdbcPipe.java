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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * @author neals
 * @since 5/31/2022
 */
@Service
public class JdbcPipe {
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
        System.out.printf("Fetch size %s, max rows %s, timeout %s%n", jdbcTemplate.getFetchSize(), jdbcTemplate.getMaxRows(), jdbcTemplate.getQueryTimeout());
        // connection size and status
        HikariDataSource ds = (HikariDataSource) jdbcTemplate.getDataSource();
        System.out.println(ds);
    }

    /**
     * init data to index
     * @param indexConfig index config
     */
    public void init(Map<String, String> indexConfig) {
        String indexName = indexConfig.get("indexName");
        String initSql = getSql(indexConfig.get("initSqlPath"));

        LocalDateTime currentRefreshTime = LocalDateTime.now();
        espipeTimer.reset(indexName, currentRefreshTime);

        jdbcMetrics();

        StopWatch sw = new StopWatch();
        sw.start();
        jdbcTemplate.query(initSql,
                new Object[]{currentRefreshTime},
                new int[]{JDBCType.TIMESTAMP.getVendorTypeNumber()},
                rs -> {
                    createDocument(indexConfig, rs);
                });
        sw.stop();
        System.out.println("total query " + sw.getTotalTimeSeconds());
    }

    /**
     * sync data to index
     * @param indexConfig index config
     */
    // todo this is supposed to be scheduled
    public void sync(Map<String, String> indexConfig) {
        String indexName = indexConfig.get("indexName");
        String syncSql = getSql(indexConfig.get("syncSqlPath"));

        // get the last refresh time from the database
        LocalDateTime lastRefreshTime = espipeTimer.findLastRefreshTime(indexName);
        LocalDateTime currentRefreshTime = LocalDateTime.now();
        espipeTimer.reset(indexName, currentRefreshTime);

        jdbcTemplate.query(conn -> {
            final PreparedStatement ps = conn.prepareStatement(syncSql);
            ParameterMetaData parameterMetaData = ps.getParameterMetaData();
            int paramCount = parameterMetaData.getParameterCount();
            System.out.println("paramCount " + paramCount);

            for (int i = 0; i < paramCount; i++) {
                if (paramCount % 2 == 0) {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(lastRefreshTime));
                } else {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(currentRefreshTime));
                }
            }
            return ps;
        }, rs -> {
             createDocument(indexConfig, rs);
        });

    }

    // TODO: create document one by one is slow, try batch update / es bulk api
    private void createDocument(Map<String, String> indexConfig, ResultSet rs) throws SQLException {
        String indexName = indexConfig.get("indexName");
        String extensionColumn = indexConfig.get("extensionColumn");
        String extensionSql = getSql(indexConfig.get("extensionSqlPath"));
        String[] idColumns = indexConfig.get("idColumns").split(",");

        // put stand fields and custom fields in flattenMap
        Map<String, Object> flattenMap = new HashMap<>();

        // prepare standard fields
        ResultSetMetaData rsMetaData = rs.getMetaData();
        int count = rsMetaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            flattenMap.put(rsMetaData.getColumnName(i).toLowerCase(), rs.getObject(i));
        }

        // prepare custom fields
        if (extensionColumn != null && flattenMap.get(extensionColumn.toLowerCase()) != null) {
            if (EspipeFieldsMode.flatten.toString().equals(espipeElasticsearchProperties.getFieldsMode())) {
                jdbcTemplate.query(extensionSql,
                        new Object[]{flattenMap.get(extensionColumn)},
                        new int[]{JDBCType.NUMERIC.getVendorTypeNumber()},
                        prs -> {
                            ResultSetMetaData prsMetaData = prs.getMetaData();
                            int pcount = prsMetaData.getColumnCount();
                            for (int i = 1; i <= pcount; i++) {
                                 flattenMap.put(prs.getString(1).toLowerCase(), prs.getObject(2));
                            }
                        });
            } else if (EspipeFieldsMode.custom_in_one.toString().equals(espipeElasticsearchProperties.getFieldsMode())) {
                StringBuilder sb = new StringBuilder();
                jdbcTemplate.query(extensionSql,
                        new Object[]{flattenMap.get(extensionColumn)},
                        new int[]{JDBCType.NUMERIC.getVendorTypeNumber()},
                        prs -> {
                            ResultSetMetaData prsMetaData = prs.getMetaData();
                            int pcount = prsMetaData.getColumnCount();
                            for (int i = 1; i <= pcount; i++) {
                                sb.append(prs.getString(1).toLowerCase());
                                sb.append(" ");
                                sb.append(prs.getObject(2));
                                sb.append(" ");
                            }
                        });
                flattenMap.put("custom_fields", sb.toString());
            }

        }

        elasticsearchPipe.createDocument(indexName, idColumns, flattenMap);
    }

    private String getSql(String sqlPath) {
        // TODO: optimize io speed and put it in cache
        // https://howtodoinjava.com/java/io/java-read-file-to-string-examples/
        // https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
        // https://www.baeldung.com/convert-input-stream-to-string

        if (sqlPath == null) {
            throw new EspipeException("initSql is not found by " + sqlPath);
        }

        InputStream initSqlIns = Thread.currentThread().getContextClassLoader().getResourceAsStream(sqlPath);
        if (initSqlIns == null) {
            throw new EspipeException("initSql is not found by " + sqlPath);
        }

        return new BufferedReader(
                new InputStreamReader(initSqlIns, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
