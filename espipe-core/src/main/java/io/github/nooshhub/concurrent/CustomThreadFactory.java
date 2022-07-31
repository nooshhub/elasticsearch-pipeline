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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Thread Factory.
 *
 * @author Neal Shan
 * @since 6/13/2022
 */
public class CustomThreadFactory implements ThreadFactory {

    public static final Logger logger = LoggerFactory.getLogger(CustomThreadFactory.class);

    private final ThreadGroup group;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public CustomThreadFactory(String prefix) {
        this.group = Thread.currentThread().getThreadGroup();
        this.namePrefix = prefix + "-t-";
        this.uncaughtExceptionHandler = new CustomUncaughtExceptionHandler();
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        t.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return t;
    }

    private static class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            logger.error("Thread {} throws an exception {}", thread.getName(), throwable.getMessage());
        }
    }

}
