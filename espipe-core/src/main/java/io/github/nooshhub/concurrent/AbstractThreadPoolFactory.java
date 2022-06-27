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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool factory, init and sync thread ratio is 3:1. TODO: optimize this later
 * https://stackoverflow.com/questions/1250643/how-to-wait-for-all-threads-to-finish-using-executorservice
 * https://www.baeldung.com/java-executor-wait-for-threads
 * https://stackoverflow.com/questions/50151816/is-it-possible-to-call-a-spring-scheduled-method-manually
 *
 * @author Neal Shan
 * @since 6/12/2022
 */
public abstract class AbstractThreadPoolFactory {

    public static ThreadPoolExecutor poolForInit() {
        final int nThreads = Runtime.getRuntime().availableProcessors() / 5 + 3;
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new CustomThreadFactory("init"));
    }

    public static ThreadPoolExecutor poolForSync() {
        final int nThreads = Runtime.getRuntime().availableProcessors() / 5 + 1;
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new CustomThreadFactory("sync"));
    }

}
