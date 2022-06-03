package io.github.nooshhub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

/**
 * @author neals
 * @since 5/31/2022
 */
@SpringBootTest
@ActiveProfiles("h2")
public class JdbcPipeTests {

    @Autowired
    private JdbcPipe jdbcPipe;
    @Autowired
    private EspipeSampleWorker espipeSampleWorker;

    @Test
    public void init() {
        IndexConfigRegistry.getInstance()
                .getIndexConfigs()
                .forEach(indexConfig -> {
                    jdbcPipe.createIndex(indexConfig);
                    jdbcPipe.init(indexConfig);
                });
    }

    @Test
    public void sync() {
        IndexConfigRegistry.getInstance()
                .getIndexConfigs()
                .forEach(indexConfig -> {
                    espipeSampleWorker.create();
                    jdbcPipe.sync(indexConfig);
                });
    }
}
