package io.github.nooshhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
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

    public LocalDateTime findLastRefreshTime(String indexName) {
        try {
            return jdbcTemplate.queryForObject(FIND_LAST_REFRESH_TIME_SQL, LocalDateTime.class, indexName);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
