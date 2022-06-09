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

package io.github.nooshhub;

import java.text.SimpleDateFormat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Elasticsearch Java client Configuration.
 *
 * @author Neal Shan
 * @since 5/31/2022
 */
@Configuration
public class ElasticsearchConfiguration {

	@Autowired
	private EspipeElasticsearchProperties espipeElasticsearchProperties;

	/**
	 * create a ElasticsearchClient java bean.
	 * @return elasticsearchClient java bean
	 */
	@Bean
	public ElasticsearchClient esClient() {
		// Create the low-level client
		RestClient restClient = RestClient
				.builder(new HttpHost(this.espipeElasticsearchProperties.getHost(), this.espipeElasticsearchProperties.getPort(),
						this.espipeElasticsearchProperties.getProtocol()))
				// enable keepalive to 300s, to be less than the ELB idle time 350s
				// so client can close connection first
				.setHttpClientConfigCallback(
						(httpClientBuilder) -> httpClientBuilder.setKeepAliveStrategy((response, context) -> 300000)
								.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build()))
				// raise the connection timeout from 1s to 5s to exclude the timeout issue
				.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
					@Override
					public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
						return requestConfigBuilder.setConnectTimeout(5000);
					}
				}).build();

		// Create the transport with a Jackson mapper
		final JacksonJsonpMapper mapper = new JacksonJsonpMapper();
		mapper.objectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'"));
		ElasticsearchTransport transport = new RestClientTransport(restClient, mapper);

		// And create the API client
		return new ElasticsearchClient(transport);
	}

}
