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

package io.github.nooshhub.metric;

/**
 * JDBC metrics to help tracking JDBC connection pool status.
 *
 * @author Neal Shan
 * @since 7/2/2022
 */
public class JdbcMetric {

    private int fetchSize = -1;

    private int maxRows = -1;

    private int queryTimeout = -1;

    private int maxPoolSize = -1;

    private int activeConnections = -1;

    public int getFetchSize() {
        return this.fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getMaxRows() {
        return this.maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getQueryTimeout() {
        return this.queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public int getMaxPoolSize() {
        return this.maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getActiveConnections() {
        return this.activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    @Override
    public String toString() {
        return "JdbcMetric{" + "fetchSize=" + this.fetchSize + ", maxRows=" + this.maxRows + ", queryTimeout="
                + this.queryTimeout + ", maxPoolSize=" + this.maxPoolSize + ", activeConnections="
                + this.activeConnections + '}';
    }

}
