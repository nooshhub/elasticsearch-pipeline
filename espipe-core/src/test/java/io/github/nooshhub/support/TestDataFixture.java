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

package io.github.nooshhub.support;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * A testing fixture for preparing data.
 *
 * @author Neal Shan
 * @since 6/3/2022
 */
@Service
public class TestDataFixture {

	private static final Logger logger = LoggerFactory.getLogger(TestDataFixture.class);

	private static final String INSERT_PROJECT = "insert into nh_project values (?, ?, ?, ?, ?)";

	private static final String INSERT_ESTIAMTE = "insert into nh_estimate values (?, ?, ?, ?, ?)";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// preserve id form 1 to 9 to manually create data
	private final AtomicInteger atomicInteger = new AtomicInteger(10);

	public void createProjects(int size) {
		List<Object[]> data = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			Object[] args = new Object[5];
			args[0] = this.atomicInteger.getAndIncrement();
			args[1] = "project " + args[0];
			args[2] = "1,2,3";
			args[3] = Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault()));
			args[4] = null;
			data.add(args);
		}

		this.jdbcTemplate.batchUpdate(INSERT_PROJECT, data);

		if (logger.isDebugEnabled()) {
			logger.debug("data from {} is prepared for {} from {} to {}", data.get(0)[0], "nh_project", data.get(0)[3],
					data.get(9)[3]);
		}
	}

	public void createEstimates(int size) {
		List<Object[]> data = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			Object[] args = new Object[5];
			args[0] = this.atomicInteger.getAndIncrement();
			args[1] = "estimate " + args[0];
			args[2] = "1,2,3";
			args[3] = Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault()));
			args[4] = null;
			data.add(args);
		}

		this.jdbcTemplate.batchUpdate(INSERT_ESTIAMTE, data);

		if (logger.isDebugEnabled()) {
			logger.debug("data from {} is prepared for {} from {} to {}", data.get(0)[0], "nh_estimate", data.get(0)[3],
					data.get(9)[3]);
		}
	}

}
