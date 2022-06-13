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

/**
 * Init index thread.
 *
 * @author Neal Shan
 * @since 6/12/2022
 */
public class SyncIndexThread implements Runnable {

	private final JdbcPipe jdbcPipe;

	private final String indexName;

	public SyncIndexThread(JdbcPipe jdbcPipe, String indexName) {
		this.jdbcPipe = jdbcPipe;
		this.indexName = indexName;
	}

	@Override
	public void run() {
		this.jdbcPipe.sync(this.indexName);
	}

}