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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.nooshhub.common.metric.Metrics;
import io.github.nooshhub.concurrent.AbstractThreadPoolFactory;
import io.github.nooshhub.concurrent.InitThread;
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
public class InitIndexService {

    private static final Logger log = LoggerFactory.getLogger(InitIndexService.class);

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private JdbcDao jdbcDao;

    private final ThreadPoolExecutor executorService = AbstractThreadPoolFactory.poolForInit();

    public List<String> init() {
        List<String> messages = new ArrayList<>();
        this.indexConfigRegistry.getIndexConfigs().keySet()
                .forEach(indexName -> messages.add(this.init(indexName)));
        return messages;
    }

    public String init(String indexName) {
        if (InitSyncManager.getInitInProgress().containsKey(indexName)) {
            final String message = String.format("Index %s is in init progress, please stop it manually.", indexName);
            log.info(message);
            return message;
        }

        Future<?> future = this.executorService.submit(new InitThread(this.jdbcDao, indexName));

        InitSyncManager.getInitInProgress().put(indexName, future);

        final String message = String.format("Init index %s is in progress", indexName);
        log.info(message);
        return message;
    }

    public String stop() {
        InitSyncManager.getInitInProgress().values().forEach(future -> future.cancel(true));
        InitSyncManager.getInitInProgress().clear();
        final String message = "Shutdown all init";
        log.info(message);
        return message;
    }

    public String stop(String indexName) {
        if (InitSyncManager.getInitInProgress().containsKey(indexName)) {
            InitSyncManager.getInitInProgress().get(indexName).cancel(true);
            InitSyncManager.getInitInProgress().remove(indexName);

            final String message = String.format("Remove index %s from init in progress", indexName);
            log.info(message);
            return message;
        } else {
            final String message = String.format("Index %s is not in init in progress", indexName);
            log.info(message);
            return message;
        }
    }

    public Metrics getMetrics() {
        Metrics metrics = new Metrics();
        metrics.setInitInProgress(InitSyncManager.getInitInProgress().keySet());
        metrics.setJdbcMetric(this.jdbcDao.jdbcMetrics());
        metrics.setThreadPoolMetric(this.executorService.toString());
        return metrics;
    }
}
