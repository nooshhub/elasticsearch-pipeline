/*
 *  Copyright 2021-2022 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.github.nooshhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Espipe Timer is using to log the last refresh time, so we can recover from a crash
 *
 * @author neals
 * @since 6/3/2022
 */
@Service
public class EspipeTimer {

    private final static String FIND_LAST_REFRESH_TIME_SQL = "select last_refresh_time from espipe_timer where index_name = ? ";
    private final static String INSERT = "insert into espipe_timer values (?,?)";
    private final static String UPDATE = "update espipe_timer set last_refresh_time = ? where index_name = ?";


    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * rest the last refresh time by index name and current refresh time
     *
     * @param indexName          index name
     * @param currentRefreshTime current refresh time
     */
    public void reset(String indexName, LocalDateTime currentRefreshTime) {
        LocalDateTime lastRefreshTime = findLastRefreshTime(indexName);
        if (lastRefreshTime == null) {
            jdbcTemplate.update(INSERT, indexName, currentRefreshTime);
        } else {
            jdbcTemplate.update(UPDATE, currentRefreshTime, indexName);
        }
    }

    /**
     * find last refresh time by index name
     *
     * @param indexName index name
     * @return last refresh time
     */
    public LocalDateTime findLastRefreshTime(String indexName) {
        try {
            return jdbcTemplate.queryForObject(FIND_LAST_REFRESH_TIME_SQL, LocalDateTime.class, indexName);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
