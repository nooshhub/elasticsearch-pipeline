package io.github.nooshhub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author neals
 * @since 5/31/2022
 */
@SpringBootTest
@ActiveProfiles("h2")
public class JdbcPipeTests {

    @Autowired
    private JdbcPipe jdbcPipe;

    @Test
    public void create() {
        jdbcPipe.create();
    }
}
