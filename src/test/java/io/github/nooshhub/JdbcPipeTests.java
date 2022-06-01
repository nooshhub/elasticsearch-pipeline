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
    public void createIndex() {
        String indexName = "nh_project";
        jdbcPipe.createIndex(indexName);
    }

    @Test
    public void createDocument() {
        String indexName = "nh_project";
        String customColumn = "nh_project_id";
        jdbcPipe.createDocument(indexName, customColumn);
    }
}
