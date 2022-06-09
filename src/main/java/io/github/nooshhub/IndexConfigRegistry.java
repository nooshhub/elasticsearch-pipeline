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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Index Config Registry
 *
 * @author Neal Shan
 * @since 6/2/2022
 */
@Service
public class IndexConfigRegistry {

	private static final Logger logger = LoggerFactory.getLogger(IndexConfigRegistry.class);

	@Value("${spring.profiles.active:h2}")
	private String profile;

	@Autowired
	private JdbcPipe jdbcPipe;

	public static final String ROOT_DIR = "espipe/";

	public static final String INDEX_CONFIG_LOCATION = "es/";

	private final List<Map<String, String>> configs = new ArrayList<>();

	public List<Map<String, String>> getIndexConfigs() {
		return configs;
	}

	@PostConstruct
	public void init() {
		scanIndexConfigs();

		if (profile.equals("h2")) {
			getIndexConfigs().forEach(indexConfig -> {
				jdbcPipe.init(indexConfig);
			});
		}
	}

	private void scanIndexConfigs() {
		String rootDir = ROOT_DIR + profile + "/" + INDEX_CONFIG_LOCATION;

		logger.info("Scanning Index Config under {}", rootDir);

		List<String> indexNames = findIndexNames(rootDir);

		for (String indexName : indexNames) {
			Map<String, String> config = new HashMap<>();
			config.put("indexName", indexName);

			// add index settings and mappings
			config.put("indexSettingsPath", rootDir + indexName + "/settings.json");
			config.put("indexMappingPath", rootDir + indexName + "/mapping.json");

			// add sql
			config.put("initSql", IOUtils.getContent(rootDir + indexName + "/sql/init.sql"));
			config.put("syncSql", IOUtils.getContent(rootDir + indexName + "/sql/sync.sql"));
			config.put("deleteSql", IOUtils.getContent(rootDir + indexName + "/sql/delete.sql"));
			config.put("extensionSql", IOUtils.getContent(rootDir + indexName + "/sql/extension.sql"));

			// add sql.properties
			String sqlPropertiesPath = rootDir + indexName + "/sql/sql.properties";
			Properties sqlProperties = new Properties();
			try (InputStream sqlPropertiesIns = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(sqlPropertiesPath)) {
				sqlProperties.load(sqlPropertiesIns);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			config.put("idColumns", sqlProperties.getProperty("id_columns"));
			config.put("extensionColumn", sqlProperties.getProperty("extension_column"));

			configs.add(config);
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
		catch (URISyntaxException e) {
			e.printStackTrace();
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
