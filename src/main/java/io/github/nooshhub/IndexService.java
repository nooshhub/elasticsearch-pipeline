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

package io.github.nooshhub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Index Service.
 * @author Neal Shan
 * @since 6/12/2022
 */
@Service
public class IndexService {

	private static final Logger logger = LoggerFactory.getLogger(IndexService.class);

	@Autowired
	private IndexConfigRegistry indexConfigRegistry;

	@Autowired
	private JdbcPipe jdbcPipe;

	private final ExecutorService executorService = AbstractThreadPoolFactory.poolForInit();

	public void init() {
		// TODO: if sync thread is exist, shut it down first

		this.indexConfigRegistry.getIndexConfigs().keySet()
				.forEach((indexName) -> this.executorService.execute(new InitIndexThread(this.jdbcPipe, indexName)));
		this.executorService.shutdown();
		try {
			this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException ex) {
			logger.warn("Interrupted!");
			// Restore interrupted state...
			Thread.currentThread().interrupt();
		}
	}

}