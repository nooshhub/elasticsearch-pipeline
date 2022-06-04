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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.AcknowledgedResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
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

    /**
     * init data to index
     * @param indexConfig index config
     */
    public void init(Map<String, String> indexConfig) {
        String indexName = indexConfig.get("indexName");
        String initSql = getSql(indexConfig.get("initSqlPath"));

        LocalDateTime currentRefreshTime = LocalDateTime.now();
        espipeTimer.reset(indexName, currentRefreshTime);
        
        jdbcTemplate.query(initSql,
                new Object[]{currentRefreshTime},
                new int[]{JDBCType.TIMESTAMP.getVendorTypeNumber()},
                rs -> {
                    createDocument(indexConfig, rs);
                });
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

        jdbcTemplate.query(syncSql,
                new Object[]{lastRefreshTime, currentRefreshTime},
                new int[]{JDBCType.TIMESTAMP.getVendorTypeNumber(), JDBCType.TIMESTAMP.getVendorTypeNumber()},
                rs -> {
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
            jdbcTemplate.query(extensionSql,
                    new Object[]{flattenMap.get(extensionColumn)},
                    new int[]{JDBCType.NUMERIC.getVendorTypeNumber()},
                    prs -> {
                        ResultSetMetaData prsMetaData = prs.getMetaData();
                        int pcount = prsMetaData.getColumnCount();
                        for (int i = 1; i <= pcount; i++) {
                            flattenMap.put(prs.getString(1).toLowerCase(), prs.getObject(i));
                        }
                    });
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
