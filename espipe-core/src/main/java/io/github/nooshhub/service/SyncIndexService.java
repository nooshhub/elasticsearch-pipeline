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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.nooshhub.common.metric.Metrics;
import io.github.nooshhub.concurrent.AbstractThreadPoolFactory;
import io.github.nooshhub.concurrent.SyncTask;
import io.github.nooshhub.concurrent.TaskManager;
import io.github.nooshhub.config.IndexConfigRegistry;
import io.github.nooshhub.dao.JdbcDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Espipe Scheduler is used to trigger synchronization task.
 *
 * @author Neal Shan
 * @since 6/4/2022
 */
@Service
public class SyncIndexService {

    private static final Logger logger = LoggerFactory.getLogger(SyncIndexService.class);

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private JdbcDao jdbcDao;

    private final ScheduledThreadPoolExecutor executorService = AbstractThreadPoolFactory.poolForSync();

    public List<String> sync() {
        List<String> messages = new ArrayList<>();
        this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> messages.add(this.sync(indexName)));
        return messages;
    }

    public String sync(String indexName) {
        if (TaskManager.getInitInProgress().containsKey(indexName)) {
            final String message = String.format("Index %s is in init progress, skip sync.", indexName);
            logger.info(message);
            return message;
        }

        if (TaskManager.getSyncInProgress().containsKey(indexName)) {
            final String message = String.format("Index %s is in sync progress, skip sync.", indexName);
            logger.info(message);
            return message;
        }

        ScheduledFuture scheduledFuture = this.executorService
                .scheduleWithFixedDelay(new SyncTask(this.jdbcDao, indexName), 1000, 5000, TimeUnit.MILLISECONDS);

        TaskManager.getSyncInProgress().put(indexName, scheduledFuture);

        final String message = String.format("Sync index %s is in progress", indexName);
        logger.info(message);
        return message;
    }

    public String stop() {
        TaskManager.getSyncInProgress().values().forEach((future) -> future.cancel(true));
        TaskManager.getSyncInProgress().clear();
        final String message = "Shutdown all sync";
        logger.info(message);
        return message;
    }

    public String stop(String indexName) {
        if (TaskManager.getSyncInProgress().containsKey(indexName)) {
            TaskManager.getSyncInProgress().get(indexName).cancel(true);
            TaskManager.getSyncInProgress().remove(indexName);

            final String message = String.format("Remove index %s from sync in progress", indexName);
            logger.info(message);
            return message;
        }
        else {
            final String message = String.format("Index %s is not in sync in progress", indexName);
            logger.info(message);
            return message;
        }
    }

    public Metrics getMetrics() {
        Metrics metrics = new Metrics();
        metrics.setInitInProgress(TaskManager.getSyncInProgress().keySet());
        metrics.setJdbcMetric(this.jdbcDao.jdbcMetrics());
        metrics.setThreadPoolMetric(this.executorService.toString());
        return metrics;
    }

}
