package io.github.nooshhub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author neals
 * @since 6/2/2022
 */
// TODO: what about a resource loader to load resources instead?
@Service
public class IndexConfigRegistry {

    @Value("${spring.profiles.active:h2}")
    private String profile;

    public static final String ROOT_DIR = "espipe/";
    public static final String INDEX_CONFIG_LOCATION = "es/";

    private final List<Map<String, String>> configs = new ArrayList<>();

    public List<Map<String, String>> getIndexConfigs() {
        return configs;
    }

    @PostConstruct
    public void init() {
        String rootDir = ROOT_DIR + profile + "/" + INDEX_CONFIG_LOCATION;

        System.out.println("Scanning Index Config under " + rootDir);

        List<String> indexNames = findIndexNames(rootDir);

        for (String indexName : indexNames) {
            Map<String, String> config = new HashMap<>();
            config.put("indexName", indexName);

            // 2 add child folder settings and mappings
            // TODO: validation: not found exception
            config.put("indexSettingsPath", rootDir + indexName + "/settings.json");
            config.put("indexMappingPath", rootDir + indexName + "/mapping.json");

            // 3 add sql folder's sql and sql.properties
            // TODO: validation: not found exception
            config.put("initSqlPath", rootDir + indexName + "/sql/init.sql");
            config.put("syncSqlPath", rootDir + indexName + "/sql/sync.sql");
            config.put("deleteSqlPath", rootDir + indexName + "/sql/delete.sql");
            config.put("extensionSqlPath", rootDir + indexName + "/sql/extension.sql");

            // TODO: validation: not found exception
            String sqlPropertiesPath = rootDir + indexName + "/sql/sql.properties";
            Properties sqlProperties = new Properties();
            try (InputStream sqlPropertiesIns = Thread.currentThread().getContextClassLoader().getResourceAsStream(sqlPropertiesPath)) {
                sqlProperties.load(sqlPropertiesIns);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // TODO: validation: not found exception
            config.put("idColumns", sqlProperties.getProperty("id_columns"));
            config.put("extensionColumn", sqlProperties.getProperty("extension_column"));

            configs.add(config);
        }

    }

    private List<String> findIndexNames(String rootDir) {
        File indexConfigRootDir = null;
        try {
            final URL resource = getClass().getClassLoader().getResource(rootDir);
            if (resource == null) {
                throw new EspipeException("Directory is not found by " + rootDir);
            }
            indexConfigRootDir = new File(resource.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (indexConfigRootDir == null) {
            throw new EspipeException("Directory is not found by " + rootDir);
        }

        List<String> indexNames = new ArrayList<>();
        for (File indexConfigDir : indexConfigRootDir.listFiles()) {
            System.out.println("Found index " + indexConfigDir.getName());
            indexNames.add(indexConfigDir.getName());
        }
        return indexNames;
    }
}
