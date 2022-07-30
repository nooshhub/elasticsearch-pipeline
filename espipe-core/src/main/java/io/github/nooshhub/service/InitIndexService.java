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
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PreDestroy;

import io.github.nooshhub.common.metric.IndexMetric;
import io.github.nooshhub.common.metric.Metrics;
import io.github.nooshhub.concurrent.AbstractThreadPoolFactory;
import io.github.nooshhub.concurrent.InitTask;
import io.github.nooshhub.concurrent.TaskManager;
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

    private static final Logger logger = LoggerFactory.getLogger(InitIndexService.class);

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private JdbcDao jdbcDao;

    private final ThreadPoolExecutor executorService = AbstractThreadPoolFactory.poolForInit();

    public List<String> init() {
        List<String> messages = new ArrayList<>();
        this.indexConfigRegistry.getIndexConfigs().keySet().forEach((indexName) -> messages.add(this.init(indexName)));
        return messages;
    }

    public String init(String indexName) {
        Future future = TaskManager.getInitInProgress().get(indexName);
        if (future != null && !future.isDone()) {
            final String message = String.format("Index %s is in init progress, please stop it manually.", indexName);
            logger.info(message);
            return message;
        }

        StringBuilder sb = new StringBuilder();
        if (TaskManager.getSyncInProgress().containsKey(indexName)) {
            TaskManager.getSyncInProgress().get(indexName).cancel(true);
            TaskManager.getSyncInProgress().remove(indexName);

            final String message = String.format("Remove index %s from sync in progress", indexName);
            logger.info(message);
            sb.append(message);
        }

        Future<?> newFuture = this.executorService.submit(new InitTask(this.jdbcDao, indexName));
        TaskManager.getInitInProgress().put(indexName, newFuture);

        final String message = String.format("Init index %s is in progress", indexName);
        logger.info(message);
        sb.append(message);
        return sb.toString();
    }

    /**
     * Init one index item by ids and values in a map, not matter if there is a init or
     * sync task in progress, just fire a init task.
     * @param indexName index name
     * @param idAndValueMap id and value map
     * @return message of process
     */
    public String init(String indexName, Map<String, String> idAndValueMap) {
        StringBuilder sb = new StringBuilder();

        this.executorService.submit(new InitTask(this.jdbcDao, indexName, idAndValueMap));

        final String message = String.format("Init one index task %s is sent", indexName);
        logger.info(message);
        sb.append(message);
        return sb.toString();
    }

    @PreDestroy
    public String stop() {
        TaskManager.getInitInProgress().values().forEach((future) -> future.cancel(true));
        TaskManager.getInitInProgress().clear();
        final String message = "Shutdown all init";
        logger.info(message);
        return message;
    }

    public String stop(String indexName) {
        if (TaskManager.getInitInProgress().containsKey(indexName)) {
            TaskManager.getInitInProgress().get(indexName).cancel(true);
            TaskManager.getInitInProgress().remove(indexName);

            final String message = String.format("Remove index %s from init in progress", indexName);
            logger.info(message);
            return message;
        }
        else {
            final String message = String.format("Index %s is not in init in progress", indexName);
            logger.info(message);
            return message;
        }
    }

    public Metrics getMetrics() {
        Metrics metrics = new Metrics();
        List<IndexMetric> indexMetrics = new ArrayList<>();
        TaskManager.getInitInProgress().forEach((indexName, future) -> {
            IndexMetric indexMetric = new IndexMetric();
            indexMetric.setIndexName(indexName);
            indexMetric.setIsDone(future.isDone());
            indexMetric.setIsCancelled(future.isCancelled());
            indexMetrics.add(indexMetric);
        });
        metrics.setIndexMetrics(indexMetrics);
        metrics.setJdbcMetric(this.jdbcDao.jdbcMetrics());
        metrics.setThreadPoolMetric(this.executorService.toString());
        return metrics;
    }

}
