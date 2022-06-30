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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Neal Shan
 * @since 6/28/2022
 */
public class SyncThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger log = LoggerFactory.getLogger(SyncThreadPoolExecutor.class);

    private final Map<String, SyncThread> tasks = new ConcurrentHashMap<>();

    public SyncThreadPoolExecutor(int corePoolSize,
                                  ThreadFactory threadFactory) {
        super(corePoolSize,
                threadFactory);
    }

    protected void beforeExecute(Thread t, Runnable r) {
        final SyncThread task = (SyncThread) r;
        this.tasks.put(task.getIndexName(), task);
        log.info("Start Sync index {} task, thread {}", task.getIndexName(), t.getName());
    }

    protected void afterExecute(Runnable r, Throwable t) {
        if (r instanceof InitThread) {
            tasks.remove(((InitThread)r).getIndexName());
        }
    }

    public void stop(String indexName) {
        SyncThread task = this.tasks.get(indexName);
        if (task == null) {
            return;
        }
        
        task.stop();
        this.tasks.remove(indexName);
        log.info("Stop Sync index {} task", indexName);
    }

    public void showMetrics() {
        this.tasks.forEach((indexName, t) -> {
            log.info("Metrics: Sync index {} ", indexName);
        });
    }
}
