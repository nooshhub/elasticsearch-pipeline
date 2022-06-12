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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Index Config Registry.
 *
 * @author Neal Shan
 * @since 6/2/2022
 */
@Service
public class IndexConfigRegistry {

	private static final Logger logger = LoggerFactory.getLogger(IndexConfigRegistry.class);

	/**
	 * root directory under resources.
	 */
	public static final String ROOT_DIR = "espipe/";

	/**
	 * directory to put create index required configuration and sql.
	 */
	public static final String INDEX_CONFIG_LOCATION = "es/";

	private static final String INDEX_SETTINGS_NAME = "/settings.json";

	private static final String INDEX_MAPPING_NAME = "/mapping.json";

	private static final String INIT_SQL_NAME = "/sql/init.sql";

	private static final String SYNC_SQL_NAME = "/sql/sync.sql";

	private static final String DELETE_SQL_NAME = "/sql/delete.sql";

	private static final String EXTENSION_SQL_NAME = "/sql/extension.sql";

	private static final String SQL_PROPERTIES_NAME = "/sql/sql.properties";

	private static final String ID_COLUMNS_NAME = "id_columns";

	private static final String EXTENSION_COLUMN_NAME = "extension_column";

	@Value("${spring.profiles.active:h2}")
	private String profile;

	private final Map<String, IndexConfig> configs = new HashMap<>();

	public Map<String, IndexConfig> getIndexConfigs() {
		return this.configs;
	}

	/**
	 * get index config.
	 * @param indexName index Name
	 * @return index config
	 */
	public IndexConfig getIndexConfig(String indexName) {
		if (this.configs.containsKey(indexName)) {
			return this.configs.get(indexName);
		}
		else {
			throw new EspipeException(String.format("Index %s is not exist under /espipe", indexName));
		}
	}

	@PostConstruct
	public void init() {
		scanIndexConfigs();
	}

	private void scanIndexConfigs() {
		String rootDir = ROOT_DIR + this.profile + "/" + INDEX_CONFIG_LOCATION;

		logger.info("Scanning Index Config under {}", rootDir);

		List<String> indexNames = findIndexNames(rootDir);

		for (String indexName : indexNames) {
			IndexConfig config = new IndexConfig();
			config.setIndexName(indexName);

			// add index settings and mappings
			config.setIndexSettingsPath(rootDir + indexName + INDEX_SETTINGS_NAME);
			config.setIndexMappingPath(rootDir + indexName + INDEX_MAPPING_NAME);

			// add sql
			config.setInitSql(IOUtils.getContent(rootDir + indexName + INIT_SQL_NAME));
			config.setSyncSql(IOUtils.getContent(rootDir + indexName + SYNC_SQL_NAME));
			config.setDeleteSql(IOUtils.getContent(rootDir + indexName + DELETE_SQL_NAME));
			config.setExtensionSql(IOUtils.getContent(rootDir + indexName + EXTENSION_SQL_NAME));

			// add sql.properties
			String sqlPropertiesPath = rootDir + indexName + SQL_PROPERTIES_NAME;
			Properties sqlProperties = new Properties();
			try (InputStream sqlPropertiesIns = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(sqlPropertiesPath)) {
				sqlProperties.load(sqlPropertiesIns);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
			config.setIdColumns(sqlProperties.getProperty(ID_COLUMNS_NAME));
			config.setExtensionColumn(sqlProperties.getProperty(EXTENSION_COLUMN_NAME));

			this.configs.put(indexName, config);
		}
	}

	private List<String> findIndexNames(String rootDir) {
		File indexConfigRootDir = null;
		try {
			final URL resource = getClass().getClassLoader().getResource(rootDir);
			if (resource == null) {
				throw new EspipeException("Directory is not found by " + rootDir);
			}
			indexConfigRootDir = new File(resource.toURI());
		}
		catch (URISyntaxException ex) {
			ex.printStackTrace();
		}

		if (indexConfigRootDir == null) {
			throw new EspipeException("Directory is not found by " + rootDir);
		}

		List<String> indexNames = new ArrayList<>();
		for (File indexConfigDir : indexConfigRootDir.listFiles()) {
			logger.info("Found index {}", indexConfigDir.getName());
			indexNames.add(indexConfigDir.getName());
		}
		return indexNames;
	}

}
