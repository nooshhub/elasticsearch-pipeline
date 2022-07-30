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
 * Index Metric shows the status of init and sync tasks.
 *
 * @author Neal Shan
 * @since 7/8/2022
 */
public class IndexMetric {

    private String indexName;

    private Boolean isDone;

    private Boolean isCancelled;

    public String getIndexName() {
        return this.indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Boolean getIsDone() {
        return this.isDone;
    }

    public void setIsDone(Boolean done) {
        this.isDone = done;
    }

    public Boolean getIsCancelled() {
        return this.isCancelled;
    }

    public void setIsCancelled(Boolean cancelled) {
        this.isCancelled = cancelled;
    }

}
