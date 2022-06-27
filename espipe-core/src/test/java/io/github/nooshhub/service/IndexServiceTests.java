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

package io.github.nooshhub.service;

import io.github.nooshhub.config.IndexConfigRegistry;
import io.github.nooshhub.dao.ElasticsearchDao;
import io.github.nooshhub.dao.JdbcDao;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IndexServiceTests}
 *
 * @author Neal Shan
 * @since 6/13/2022
 */
@SpringBootTest(properties = "spring.profiles.active:h2")
public class IndexServiceTests {

    @Autowired
    private IndexService indexService;

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private ElasticsearchDao elasticsearchDao;

    @Autowired
    private JdbcDao jdbcDao;

    @Test
    public void init() {
        this.indexService.init();

        this.indexConfigRegistry.getIndexConfigs().keySet()
                .forEach((indexName) -> assertThat(this.elasticsearchDao.isIndexExist(indexName)).isTrue());

        this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> {
            this.elasticsearchDao.refresh(indexName);
            assertThat(this.elasticsearchDao.indexTotalCount(indexName))
                    .isEqualTo(this.jdbcDao.getTotalCount(indexName));
        });
    }

}
