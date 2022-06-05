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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * @author neals
 * @since 6/3/2022
 */
@Service
public class ElasticsearchPipe {

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
                System.out.println("Index is deleted");
            }
        } catch (ElasticsearchException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                System.out.println("Index is not exist, no operation");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream indexSettingIns = Thread.currentThread().getContextClassLoader().getResourceAsStream(settingsPath)) {
            // create index with settings
            esClient.indices().create(CreateIndexRequest.of(b -> b
                    .index(indexName)
                    .withJson(indexSettingIns)));
            System.out.println("Index is created");
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: do we need to close the client manually
            // https://github.com/elastic/elasticsearch-java/issues/104
            // TODO: DO we really need a elasticsearch-client? the httpClient is enough and we can control all behaviors as expected
        }

        try (InputStream indexMappingIns = Thread.currentThread().getContextClassLoader().getResourceAsStream(mappingPath)) {
            // update index mapping
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


    /**
     * create one document
     *
     * @param indexConfig index Config
     * @param flattenMap  flatten Map
     */
    public void createDocument(Map<String, String> indexConfig, Map<String, Object> flattenMap) {
        String indexName = indexConfig.get("indexName");
        String[] idColumns = indexConfig.get("idColumns").split(",");

        try {
            // convert to json
            final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(flattenMap);
            System.out.println(json);

            // load id from config, support columns combination strategy as id
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
    }

    /**
     * create multiple document
     *
     * @param indexConfig    index Config
     * @param flattenMapList flatten Map list
     */
    public void createDocument(Map<String, String> indexConfig, List<Map<String, Object>> flattenMapList) {
        String indexName = indexConfig.get("indexName");
        String[] idColumns = indexConfig.get("idColumns").split(",");

        try {

            List<BulkOperation> bulkOperations = new ArrayList<>();

            for (Map<String, Object> flattenMap : flattenMapList) {
                // load id from config, support columns combination strategy as id
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
            System.out.println(bulkRes);


        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // TODO: how to process this exception
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: do we need to close the client manually
            // https://github.com/elastic/elasticsearch-java/issues/104
            // TODO: DO we really need a elasticsearch-client? the httpClient is enough and we can control all behaviors as expected
        }
    }
}
