package io.github.nooshhub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


/**
 * @author neals
 * @since 5/31/2022
 */
@SpringBootTest(properties = "spring.profiles.active:oracle")
public class JdbcPipeTests {

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;
    @Autowired
    private ElasticsearchPipe elasticsearchPipe;
    @Autowired
    private JdbcPipe jdbcPipe;
    @Autowired
    private EspipeSampleWorker espipeSampleWorker;

    @Test
    public void init() {
        indexConfigRegistry.getIndexConfigs()
                .forEach(indexConfig -> {
                    elasticsearchPipe.createIndex(indexConfig);
                    jdbcPipe.init(indexConfig);
                });
    }

    @Test
    public void sync() {
        indexConfigRegistry.getIndexConfigs()
                .forEach(indexConfig -> {
                    espipeSampleWorker.create();
                    jdbcPipe.sync(indexConfig);
                });
    }
}
