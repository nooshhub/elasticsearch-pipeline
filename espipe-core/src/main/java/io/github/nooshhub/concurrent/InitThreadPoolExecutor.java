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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Neal Shan
 * @since 6/28/2022
 */
public class InitThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Logger log = LoggerFactory.getLogger(InitThreadPoolExecutor.class);

    private final Map<String, Thread> tasks = new ConcurrentHashMap<>();

    public InitThreadPoolExecutor(int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime,
                                  TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory) {
        super(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory);
    }

    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        final InitThread task = (InitThread) r;
        this.tasks.put(task.getIndexName(), t);
        log.info("Init index {} task {} start", task.getIndexName(), t.getName());
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null
                && r instanceof Future<?>
                && ((Future<?>) r).isDone()) {
            try {
                Object result = ((Future<?>) r).get();
                System.out.println(result.getClass());
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                // ignore/reset
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            System.out.println(t);
        }
    }

    public void stop(String indexName) {
        Thread t = this.tasks.get(indexName);
        t.interrupt();
        log.info("Init index {} task {} stop", indexName, t.getName());
        this.tasks.remove(indexName);
    }

    public void showMetrics() {
        this.tasks.forEach((indexName, t) -> {
            log.info("Metrics: Init index {} task {}", indexName, t.getName());
        });
    }
}
