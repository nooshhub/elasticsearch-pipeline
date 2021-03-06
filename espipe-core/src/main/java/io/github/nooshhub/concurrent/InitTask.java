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

package io.github.nooshhub.concurrent;

import java.util.Map;

import io.github.nooshhub.dao.JdbcDao;

/**
 * Init index thread.
 *
 * @author Neal Shan
 * @since 6/12/2022
 */
public class InitTask implements Runnable {

    private final JdbcDao jdbcDao;

    private final String indexName;

    private final Map<String, String> idAndValueMap;

    public InitTask(JdbcDao jdbcDao, String indexName) {
        this(jdbcDao, indexName, null);
    }

    public InitTask(JdbcDao jdbcDao, String indexName, Map<String, String> idAndValueMap) {
        this.jdbcDao = jdbcDao;
        this.indexName = indexName;
        this.idAndValueMap = idAndValueMap;
    }

    @Override
    public void run() {
        if (this.idAndValueMap == null) {
            this.jdbcDao.init(this.indexName);
        }
        else {
            this.jdbcDao.init(this.indexName, this.idAndValueMap);
        }
    }

    public String getIndexName() {
        return this.indexName;
    }

}
