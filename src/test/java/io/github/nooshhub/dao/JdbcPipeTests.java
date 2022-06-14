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

import io.github.nooshhub.config.IndexConfigRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit tests for {@link JdbcPipeTests}
 *
 * @author Neal Shan
 * @since 5/31/2022
 */
@SpringBootTest(properties = "spring.profiles.active:h2")
public class JdbcPipeTests {

	@Autowired
	private IndexConfigRegistry indexConfigRegistry;

	@Autowired
	private JdbcPipe jdbcPipe;

	@Value("${spring.profiles.active:h2}")
	private String profile;

	@Test
	public void init() {
		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> this.jdbcPipe.init(indexName));
		// TODO: add assertions to check if index is exist
		// TODO: add assertions to check if document is exist
	}

	@Test
	public void sync() {
		if (this.profile.equals("h2")) {
			this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> this.jdbcPipe.init(indexName));
		}
		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> this.jdbcPipe.sync(indexName));
		// TODO: add assertions to check if index is exist
		// TODO: add assertions to check if document is exist
	}

}
