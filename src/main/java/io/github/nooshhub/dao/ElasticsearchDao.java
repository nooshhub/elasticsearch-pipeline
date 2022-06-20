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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.AcknowledgedResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import io.github.nooshhub.config.IndexConfig;
import io.github.nooshhub.config.IndexConfigRegistry;
import io.github.nooshhub.exception.EspipeException;
import io.github.nooshhub.support.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Elasticsearch DAO, create, update, delete index or document
 * <p>
 * Document's difference between espipe and logstash 1. why logstash created index has
 * these fields @version @timestamp
 * https://discuss.elastic.co/t/deleting-version-and-timestamp-fields-in-logstash-2-2-2/169475
 * <p>
 * Known issues: https://github.com/elastic/elasticsearch-java/issues/104
 * <p>
 * Something to know: 1. _version in response
 * https://www.elastic.co/blog/elasticsearch-versioning-support
 *
 * @author Neal Shan
 * @since 6/3/2022
 */
@Service
public class ElasticsearchDao {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDao.class);

	@Autowired
	private ElasticsearchClient esClient;

	@Autowired
	private ElasticsearchAsyncClient esAsyncClient;

	@Autowired
	private IndexConfigRegistry indexConfigRegistry;

	/**
	 * create index.
	 * @param indexName index name
	 */
	public void createIndex(String indexName) {
		IndexConfig indexConfig = this.indexConfigRegistry.getIndexConfig(indexName);

		// delete index if index is exist
		try {
			// Delete index must be synchronized
			AcknowledgedResponse deleteIndexResponse = this.esClient.indices()
					.delete(DeleteIndexRequest.of((b) -> b.index(indexName)));
			if (deleteIndexResponse.acknowledged()) {
				logger.info("Index {} is deleted", indexName);
			}
		}
		catch (ElasticsearchException ex) {
			if (ex.status() == HttpStatus.NOT_FOUND.value()) {
				logger.info("Index {} is not exist, no operation", indexName);
			}
		}
		catch (IOException ex) {
			throw new EspipeException(ex.getMessage());
		}

		try (InputStream indexSettingIns = IOUtils.getInputStream(indexConfig.getIndexSettingsPath())) {
			// create index with settings
			this.esClient.indices().create(CreateIndexRequest.of((b) -> b.index(indexName).withJson(indexSettingIns)));
			logger.info("Index {} is created", indexName);
		}
		catch (IOException ex) {
			throw new EspipeException(ex.getMessage());
		}

		try (InputStream indexMappingIns = IOUtils.getInputStream(indexConfig.getIndexMappingPath())) {
			// update index mapping
			this.esClient.indices()
					.putMapping(PutMappingRequest.of((b) -> b.index(indexName).withJson(indexMappingIns)));
			logger.info("Index {} mapping is updated", indexName);
		}
		catch (IOException ex) {
			throw new EspipeException(ex.getMessage());
		}
	}

	/**
	 * Update Settings After Init is finished. PUT /my-index-000001/_settings {"index" :
	 * {"refresh_interval" : "1s"}}
	 * @param indexName index name
	 */
	public void updateSettingsAfterInit(String indexName) {
		logger.info("Update index settings to refresh_interval 1s");
		try {
			this.esClient.indices().putSettings(PutIndicesSettingsRequest.of(
					(b) -> b.index(indexName).settings((settings) -> settings.refreshInterval((t) -> t.time("1s")))));
		}
		catch (IOException ex) {
			throw new EspipeException(ex.getMessage());
		}
	}

	/**
	 * check if server is up.
	 * @return true if server is up, otherwise false
	 */
	public boolean isServerUp() {
		try {
			return this.esClient.ping().value();
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * check if index is exist.
	 * @param indexName index name
	 * @return true if index is exist, otherwise false
	 */
	public boolean isIndexExist(String indexName) {
		try {
			return this.esClient.indices().exists((builder) -> builder.index(indexName)).value();
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * create one document.
	 * @param indexName index name
	 * @param flattenMap flatten Map
	 * @return bulk response completable future
	 */
	public CompletableFuture<BulkResponse> createDocument(String indexName, Map<String, Object> flattenMap) {
		return createDocument(indexName, List.of(flattenMap));
	}

	/**
	 * create multiple document.
	 * @param indexName index name
	 * @param flattenMapList flatten Map list
	 * @return bulk response completable future
	 */
	public CompletableFuture<BulkResponse> createDocument(String indexName, List<Map<String, Object>> flattenMapList) {
		List<BulkOperation> bulkOperations = new ArrayList<>();

		for (Map<String, Object> flattenMap : flattenMapList) {
			final String documentId = getDocumentId(indexName, flattenMap);

			BulkOperation bulkOperation = BulkOperation
					.of((b) -> b.create((c) -> c.index(indexName).id(documentId).document(flattenMap)));
			bulkOperations.add(bulkOperation);
		}

		BulkRequest bulkRequest = BulkRequest.of((b) -> b.operations(bulkOperations));

		return this.esAsyncClient.bulk(bulkRequest);
	}

	/**
	 * process single bulk response completable future.
	 * @param indexName index name
	 * @param future completable future
	 */
	public void processCompletableFuture(String indexName, CompletableFuture<BulkResponse> future) {
		processCompletableFutures(indexName, List.of(future));
	}

	/**
	 * process a list of bulk response completable future.
	 * @param indexName index name
	 * @param futures a list of completable future
	 */
	public void processCompletableFutures(String indexName, List<CompletableFuture<BulkResponse>> futures) {
		logger.info("Bulk requests (size: {}) for index {}", futures.size(), indexName);
		futures.forEach((bulkResFuture) -> {
			try {
				BulkResponse bulkRes = bulkResFuture.get();
				if (bulkRes.errors()) {
					bulkRes.items().forEach((bulkResponseItem) -> {
						if (bulkResponseItem.error() != null) {
							if (("version_conflict_engine_exception").equals(bulkResponseItem.error().type())) {
								logger.warn(bulkResponseItem.error().reason());
							}
							else {
								logger.error(bulkResponseItem.error().reason());
							}
						}
					});
				}
			}
			catch (InterruptedException | ExecutionException ex) {
				logger.warn("Interrupted!");
				// Restore interrupted state...
				Thread.currentThread().interrupt();
			}
		});
	}

	/**
	 * load id from index config, support columns combination strategy as id.
	 * @param indexName index name
	 * @param flattenMap flatten Map
	 * @return document Id
	 */
	private String getDocumentId(String indexName, Map<String, Object> flattenMap) {
		IndexConfig indexConfig = this.indexConfigRegistry.getIndexConfig(indexName);
		String[] idColumns = indexConfig.getIdColumns().split(",");

		final String documentId;
		if (idColumns.length == 1) {
			documentId = flattenMap.get(idColumns[0]).toString();
		}
		else {
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
