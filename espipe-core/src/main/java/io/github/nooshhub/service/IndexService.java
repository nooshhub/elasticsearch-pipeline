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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.nooshhub.concurrent.AbstractThreadPoolFactory;
import io.github.nooshhub.concurrent.InitThread;
import io.github.nooshhub.concurrent.InitThreadPoolExecutor;
import io.github.nooshhub.config.IndexConfigRegistry;
import io.github.nooshhub.dao.JdbcDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Index Service.
 *
 * @author Neal Shan
 * @since 6/12/2022
 */
@Service
public class IndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexService.class);

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private JdbcDao jdbcDao;

    private final InitThreadPoolExecutor executorService = AbstractThreadPoolFactory.poolForInit();

    public void init() {
        this.indexConfigRegistry.getIndexConfigs().keySet()
                .forEach((indexName) -> this.executorService.execute(new InitThread(this.jdbcDao, indexName)));

        this.executorService.shutdown();
        try {
            this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex) {
            logger.warn("Interrupted!");
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

    public void init(String indexName) {
        this.executorService.execute(new InitThread(this.jdbcDao, indexName));

        this.executorService.shutdown();
        try {
            this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex) {
            logger.warn("Interrupted!");
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        this.executorService.shutdownNow();
    }

    public void stop(String indexName) {
        this.executorService.stop(indexName);
    }

    public void showMetrics() {
        this.executorService.showMetrics();
    }
}
