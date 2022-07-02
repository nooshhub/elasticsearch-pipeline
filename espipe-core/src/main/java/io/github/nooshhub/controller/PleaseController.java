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

package io.github.nooshhub.controller;

import java.util.List;

import io.github.nooshhub.common.metric.Metrics;
import io.github.nooshhub.service.InitIndexService;
import io.github.nooshhub.service.SyncIndexService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Follow the style of please command. Ex: please init all.
 *
 * @author Neal Shan
 * @since 6/26/2022
 */
@RestController
public class PleaseController {

    @Autowired
    private InitIndexService initIndexService;
    @Autowired
    private SyncIndexService syncIndexService;

    @GetMapping("please/start/init/all")
    public List<String> startInitAll() {
        return this.initIndexService.init();
    }

    @GetMapping("please/start/init/{indexName}")
    public String startInitOne(@PathVariable("indexName") String indexName) {
        return this.initIndexService.init(indexName);
    }

    @GetMapping("please/stop/init/all")
    public String stopInitAll() {
        return this.initIndexService.stop();
    }

    @GetMapping("please/stop/init/{indexName}")
    public String stopInitOne(@PathVariable("indexName") String indexName) {
        return this.initIndexService.stop(indexName);
    }

    @GetMapping("please/start/sync/all")
    public List<String> startSyncAll() {
        return this.syncIndexService.sync();
    }

    @GetMapping("please/start/sync/{indexName}")
    public String startSyncOne(@PathVariable("indexName") String indexName) {
        return this.syncIndexService.sync(indexName);
    }

    @GetMapping("please/stop/sync/all")
    public String stopSyncAll() {
        return this.syncIndexService.stop();
    }

    @GetMapping("please/stop/sync/{indexName}")
    public String stopSyncOne(@PathVariable("indexName") String indexName) {
        return this.syncIndexService.stop(indexName);
    }

    @GetMapping("please/fix/{indexName}/{id}")
    public void fixOne() {
        // TODO: if the result is exist in DB, then fix it.
    }

    @GetMapping("please/show/metrics")
    public List<Metrics> showMetrics() {
        return List.of(this.initIndexService.getMetrics(), this.syncIndexService.getMetrics());
    }

}
