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

import io.github.nooshhub.service.IndexService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Follow the style of please command.
 * Ex: please init all.
 * 
 * @author Neal Shan
 * @since 6/26/2022
 */
@RestController
public class PleaseController {

	@Autowired
	private IndexService indexService;

	@GetMapping("please/start/init/all")
	public void startInitAll() {
		indexService.init();
	}

	@GetMapping("please/start/init/{indexName}")
	public void startInitOne(@PathVariable("indexName") String indexName) {
		indexService.init(indexName);
	}

	@GetMapping("please/stop/init/all")
	public void stopInitAll() {
		// TODO: how to stop https://www.baeldung.com/java-thread-stop
	}

	@GetMapping("please/stop/init/{indexName}")
	public void stopInitOne() {

	}

	@GetMapping("please/start/sync/all")
	public void startSyncAll() {

	}

	@GetMapping("please/start/sync/{indexName}")
	public void startSyncOne() {

	}

	@GetMapping("please/stop/sync/all")
	public void stopSyncAll() {

	}

	@GetMapping("please/stop/sync/{indexName}")
	public void stopSyncOne() {

	}

	@GetMapping("please/fix/{indexName}/{id}")
	public void fixOne() {

	}

	@GetMapping("please/show/metrics")
	public void showMetrics() {

	}


}
