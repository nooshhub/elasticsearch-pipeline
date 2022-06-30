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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.nooshhub.concurrent.AbstractThreadPoolFactory;
import io.github.nooshhub.concurrent.InitThread;
import io.github.nooshhub.concurrent.SyncThread;
import io.github.nooshhub.concurrent.SyncThreadPoolExecutor;
import io.github.nooshhub.config.IndexConfigRegistry;
import io.github.nooshhub.dao.JdbcDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Espipe Scheduler is used to trigger synchronization task.
 *
 * @author Neal Shan
 * @since 6/4/2022
 */
@Service
public class SyncIndexService {

    private static final Logger log = LoggerFactory.getLogger(SyncIndexService.class);

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private JdbcDao jdbcDao;

    private final SyncThreadPoolExecutor executorService = AbstractThreadPoolFactory.poolForSync();

    public void sync() {
        stop();

        // TODO: if init thread is exist, sync should not be performed
        this.indexConfigRegistry.getIndexConfigs().keySet()
                .forEach((indexName) -> this.executorService.scheduleWithFixedDelay(
                        new SyncThread(this.jdbcDao, indexName),
                        1000,
                        5000,
                        TimeUnit.MILLISECONDS));
    }

    public void sync(String indexName) {
        stop(indexName);

        this.executorService.scheduleWithFixedDelay(
                new SyncThread(this.jdbcDao, indexName),
                1000,
                5000,
                TimeUnit.MILLISECONDS);
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
