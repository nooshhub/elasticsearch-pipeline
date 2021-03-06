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

import java.util.List;

/**
 * An aggregation of metrics.
 *
 * @author Neal Shan
 * @since 7/2/2022
 */
public class Metrics {

    private List<IndexMetric> indexMetrics;

    private JdbcMetric jdbcMetric;

    private String threadPoolMetric;

    public List<IndexMetric> getIndexMetrics() {
        return this.indexMetrics;
    }

    public void setIndexMetrics(List<IndexMetric> indexMetrics) {
        this.indexMetrics = indexMetrics;
    }

    public JdbcMetric getJdbcMetric() {
        return this.jdbcMetric;
    }

    public void setJdbcMetric(JdbcMetric jdbcMetric) {
        this.jdbcMetric = jdbcMetric;
    }

    public String getThreadPoolMetric() {
        return this.threadPoolMetric;
    }

    public void setThreadPoolMetric(String threadPoolMetric) {
        this.threadPoolMetric = threadPoolMetric;
    }

}
