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
import io.github.nooshhub.support.TestDataFixture;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcDaoTests}
 *
 * @author Neal Shan
 * @since 5/31/2022
 */
@SpringBootTest(properties = "spring.profiles.active:h2")
public class JdbcDaoTests {

	@Autowired
	private IndexConfigRegistry indexConfigRegistry;

	@Autowired
	private JdbcDao jdbcDao;

	@Autowired
	private TestDataFixture testDataFixture;

	@Autowired
	protected ElasticsearchDao elasticsearchDao;

	@Value("${spring.profiles.active:h2}")
	private String profile;

	@Test
	public void init() {
		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> this.jdbcDao.init(indexName));

		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) ->
			assertThat(this.elasticsearchDao.isIndexExist(indexName)).isTrue()
		);

		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> {
			this.elasticsearchDao.refresh(indexName);
			assertThat(this.elasticsearchDao.indexTotalCount(indexName)).isEqualTo(this.jdbcDao.getTotalCount(indexName));
		});
	}

	@Test
	public void sync() {
		if (this.profile.equals("h2")) {
			this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> this.jdbcDao.init(indexName));

			this.testDataFixture.createProjects(10);
			this.testDataFixture.createEstimates(10);
		}

		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> this.jdbcDao.sync(indexName));

		this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> {
			this.elasticsearchDao.refresh(indexName);
			assertThat(this.elasticsearchDao.indexTotalCount(indexName)).isEqualTo(this.jdbcDao.getTotalCount(indexName));
		});
	}

}
