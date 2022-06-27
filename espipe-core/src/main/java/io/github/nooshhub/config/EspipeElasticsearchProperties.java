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

package io.github.nooshhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Espipe Elasticsearch Properties.
 *
 * @author Neal Shan
 * @since 5/31/2022
 */
@Component
@ConfigurationProperties(prefix = "espipe.elasticsearch")
public class EspipeElasticsearchProperties {

    private String host;

    private int port;

    private String protocol;

    private String fieldsMode;

    /**
     * bulkSize.
     * https://www.elastic.co/blog/benchmarking-and-sizing-your-elasticsearch-cluster-for-logs-and-metrics
     */
    private int bulkSize;

    public int getBulkSize() {
        return this.bulkSize;
    }

    public void setBulkSize(int bulkSize) {
        this.bulkSize = bulkSize;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getFieldsMode() {
        return this.fieldsMode;
    }

    public void setFieldsMode(String fieldsMode) {
        this.fieldsMode = fieldsMode;
    }

}
