/*
 *  Copyright 2021-2022 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.github.nooshhub;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.AcknowledgedResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Pipe, create, update, delete index or document
 * <p>
 * Document's difference between espipe and logstash
 * 1. why logstash created index has these fields @version @timestamp
 * https://discuss.elastic.co/t/deleting-version-and-timestamp-fields-in-logstash-2-2-2/169475
 * <p>
 * Known issues:
 * https://github.com/elastic/elasticsearch-java/issues/104
 * <p>
 * Something to know:
 * 1. _version in response
 * https://www.elastic.co/blog/elasticsearch-versioning-support
 *
 * @author neals
 * @since 6/3/2022
 */
@Service
public class ElasticsearchPipe {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchPipe.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ElasticsearchClient esClient;

    /**
     * create index
     *
     * @param indexConfig index config
     */
    public void createIndex(Map<String, String> indexConfig) {
        final String indexName = indexConfig.get("indexName");
        final String settingsPath = indexConfig.get("indexSettingsPath");
        final String mappingPath = indexConfig.get("indexMappingPath");

        // delete index if index is exist
        try {
            // Delete index must be synchronized
            AcknowledgedResponse deleteIndexResponse = esClient.indices().delete(DeleteIndexRequest.of(b -> b.index(indexName)));
            if (deleteIndexResponse.acknowledged()) {
                logger.info("Index {} is deleted", indexName);
            }
        } catch (ElasticsearchException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                logger.info("Index {} is not exist, no operation", indexName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (InputStream indexSettingIns = IOUtils.getInputStream(settingsPath)) {
            // create index with settings
            esClient.indices().create(CreateIndexRequest.of(b -> b
                    .index(indexName)
                    .withJson(indexSettingIns)));
            logger.info("Index {} is created", indexName);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (InputStream indexMappingIns = IOUtils.getInputStream(mappingPath)) {
            // update index mapping
            esClient.indices().putMapping(PutMappingRequest.of(b -> b
                    .index(indexName)
                    .withJson(indexMappingIns)));
            logger.info("Index {} mapping is updated", indexName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * create one document
     *
     * @param indexConfig index Config
     * @param flattenMap  flatten Map
     */
    public void createDocument(Map<String, String> indexConfig, Map<String, Object> flattenMap) {
        String indexName = indexConfig.get("indexName");

        try {
            final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(flattenMap);
            final String documentId = getDocumentId(indexConfig, flattenMap);

            IndexRequest<JsonData> req;
            req = IndexRequest.of(b -> {
                        return b
                                .index(indexName)
                                .id(documentId)
                                .withJson(new StringReader(json));
                    }
            );

            IndexResponse res = esClient.index(req);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * create multiple document
     *
     * @param indexConfig    index Config
     * @param flattenMapList flatten Map list
     */
    public void createDocument(Map<String, String> indexConfig, List<Map<String, Object>> flattenMapList) {
        String indexName = indexConfig.get("indexName");

        try {
            List<BulkOperation> bulkOperations = new ArrayList<>();

            for (Map<String, Object> flattenMap : flattenMapList) {
                final String documentId = getDocumentId(indexConfig, flattenMap);

                BulkOperation bulkOperation = BulkOperation.of(b -> {
                    return b.create(c -> {
                        return c.index(indexName)
                                .id(documentId)
                                .document(flattenMap)
                                ;
                    });
                });
                bulkOperations.add(bulkOperation);
            }

            BulkRequest bulkRequest = BulkRequest.of(b -> {
                return b.operations(bulkOperations);
            });

            BulkResponse bulkRes = esClient.bulk(bulkRequest);
            if (logger.isDebugEnabled()) {
                logger.debug(bulkRes.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * load id from index config, support columns combination strategy as id
     *
     * @param indexConfig index config
     * @param flattenMap  flatten Map
     * @return Document Id
     */
    private String getDocumentId(Map<String, String> indexConfig, Map<String, Object> flattenMap) {
        String[] idColumns = indexConfig.get("idColumns").split(",");

        final String documentId;
        if (idColumns.length == 1) {
            documentId = flattenMap.get(idColumns[0]).toString();
        } else {
            String[] ids = new String[idColumns.length];
            for (int i = 0; i < idColumns.length; i++) {
                ids[i] = flattenMap.get(idColumns[i]).toString();
            }
            documentId = String.join("-", ids);
        }

        if (documentId == null) {
            throw new EspipeException("documentId must not be null");
        }
        return documentId;
    }
}
