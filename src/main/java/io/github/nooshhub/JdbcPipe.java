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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.JDBCType;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author neals
 * @since 5/31/2022
 */
@Service
public class JdbcPipe {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ElasticsearchClient esClient;

    public static final String INDEX_CONFIG_LOCATION ="es/";

    public Map<String, String> scanIndexConfig() {
        // TODO:
        // scan the es/ folder,
        // 1 use child folder as index name
        // 2 add child folder settings and mappings
        // 3 add sql folder's sql and extension.properties
        Map<String, String> config = new HashMap<>();
        final String indexName = "nh_project";
        config.put("indexName", indexName);
        config.put("indexSettingsPath", INDEX_CONFIG_LOCATION + indexName + "/settings.json");
        config.put("indexMappingPath", INDEX_CONFIG_LOCATION + indexName + "/mapping.json");
        config.put("initSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/init.sql");
        config.put("updateSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/update.sql");
        config.put("deleteSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/delete.sql");
        config.put("extensionSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/extension.sql");
        config.put("idColumns", "nh_project_id");
        config.put("extensionColumn", "nh_project_id");
        return config;
    }

    public void createIndex(Map<String, String> indexConfig) {

        // TODO: validation: not found exception
        final String indexName = indexConfig.get("indexName");
        final String settingsPath = indexConfig.get("indexSettingsPath");
        final String mappingPath = indexConfig.get("indexMappingPath");

        try {
            // delete index if index is exist
            try {
                // Delete index must be synchronized
                AcknowledgedResponse deleteIndexResponse = esClient.indices().delete(DeleteIndexRequest.of(b -> b.index(indexName)));
                if (deleteIndexResponse.acknowledged()) {
                    System.out.println("Index is deleted");
                }
            } catch (ElasticsearchException exception) {
                if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                    System.out.println("Index is not exist, no operation");
                }
            }

            // create index with settings
            InputStream indexSettingIns = this.getClass().getClassLoader().getResourceAsStream(settingsPath);
            esClient.indices().create(CreateIndexRequest.of(b -> b
                    .index(indexName)
                    .withJson(indexSettingIns)));
            System.out.println("Index is created");

            // update index mapping
            InputStream indexMappingIns = this.getClass().getClassLoader().getResourceAsStream(mappingPath);
            esClient.indices().putMapping(PutMappingRequest.of(b -> b
                    .index(indexName)
                    .withJson(indexMappingIns)));
            System.out.println("Index mapping is updated");


        } catch (IOException e) {
            e.printStackTrace();
            // TODO: do we need to close the client manually
            // https://github.com/elastic/elasticsearch-java/issues/104
            // TODO: DO we really need a elasticsearch-client? the httpClient is enough and we can control all behaviors as expected
        }
    }

    public void createDocument(Map<String, String> indexConfig) {

        // TODO: validation: not found exception
        String indexName = indexConfig.get("indexName");
        String extensionColumn = indexConfig.get("extensionColumn");
        // TODO: get from config
        final String initSql = "select * from nh_project";
        final String extensionSql = "SELECT attribute_key, attribute_value FROM nh_property_attribute WHERE nh_project_id = ?";

        jdbcTemplate.query(initSql, rs -> {
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

            // convert to json
            try {
                final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(flattenMap);
                System.out.println(json);

                // TODO: load id from config, support columns combination strategy as id
                final String documentId = flattenMap.get(extensionColumn).toString();
                IndexRequest<JsonData> req;
                req = IndexRequest.of(b -> {
                            return b
                                    .index(indexName)
                                    .id(documentId)
                                    .withJson(new StringReader(json));
                        }
                );

                // @version @timestamp, why logstash created index has these fields
                // https://discuss.elastic.co/t/deleting-version-and-timestamp-fields-in-logstash-2-2-2/169475
                IndexResponse res = esClient.index(req);

                // _version in response
                // https://www.elastic.co/blog/elasticsearch-versioning-support
                System.out.println(res);

            } catch (JsonProcessingException e) {
                e.printStackTrace();
                // TODO: how to process this exception
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: do we need to close the client manually
                // https://github.com/elastic/elasticsearch-java/issues/104
                // TODO: DO we really need a elasticsearch-client? the httpClient is enough and we can control all behaviors as expected
            }


        });
    }
}
