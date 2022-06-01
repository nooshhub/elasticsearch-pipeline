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
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
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


    public void create() {
        // TODO: load main table or sql, with the configed number of requests
        jdbcTemplate.query("select * from nh_project where nh_project_id < 10", rs -> {
            System.out.println(rs.getClass().getName());
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int count = rsMetaData.getColumnCount();
            Map<String, Object> map = new HashMap<>();
            for (int i = 1; i <= count; i++) {
                final String columnName = rsMetaData.getColumnName(i);
                System.out.println(columnName);
                if (rs.getObject(i) != null) {
                    System.out.println(rs.getObject(i).getClass().getName());
                }

                map.put(columnName.toLowerCase(), rs.getObject(i));

            }

            // jdbc metadata
            // put in map
            // and load custom
            final String customColumn = "nh_project_id";
            if (map.get(customColumn) != null) {
                System.out.println("load custom " + map.get(customColumn));
                // TODO: load relation config, sql
                jdbcTemplate.query("SELECT attribute_key, attribute_value FROM nh_property_attribute WHERE "  + customColumn +" = " + map.get(customColumn), prs -> {
                    ResultSetMetaData prsMetaData = prs.getMetaData();
                    int pcount = prsMetaData.getColumnCount();
                    for (int i = 1; i <= pcount; i++) {
                        map.put("custom_" + prs.getString(1).toLowerCase(), prs.getObject(i));

                    }
                });
            }

            // convert to json
            // use fieldStrategy to convert data type, like date to YYYY-DD-MM,  Long to String , String to Long
            try {
                final String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                System.out.println(json);

                // TODO: load index name from config?
                final String indexName = "test-index-2";
                // todo: check if index exist

                // todo: load custom index info, mapping, like set format for date, use custom analysis
                // reference http://10.10.21.16:9200/pm_project?pretty

                // TODO: load id from config, support columns combination as id
                final String documentId = map.get(customColumn).toString();

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
