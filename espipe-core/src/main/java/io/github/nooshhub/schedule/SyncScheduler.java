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

package io.github.nooshhub.schedule;

import java.util.concurrent.ExecutorService;

import io.github.nooshhub.concurrent.AbstractThreadPoolFactory;
import io.github.nooshhub.concurrent.SyncThread;
import io.github.nooshhub.config.IndexConfigRegistry;
import io.github.nooshhub.dao.JdbcDao;

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
public class SyncScheduler {

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;

    @Autowired
    private JdbcDao jdbcDao;

    private final ExecutorService executorService = AbstractThreadPoolFactory.poolForSync();

    @Scheduled(fixedRate = 5000)
    public void sync() {
        // TODO: if init thread is exist, sync should not be performed
        this.indexConfigRegistry.getIndexConfigs().keySet()
                .forEach((indexName) -> this.executorService.execute(new SyncThread(this.jdbcDao, indexName)));
        // TODO: shutdownhook for sync
    }

}
