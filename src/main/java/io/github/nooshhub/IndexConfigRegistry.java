package io.github.nooshhub;

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
public class IndexConfigRegistry {
    public static final String INDEX_CONFIG_LOCATION = "es/";
    private static final IndexConfigRegistry REGISTRY = new IndexConfigRegistry();
    private final List<Map<String, String>> configs = new ArrayList<>();

    private IndexConfigRegistry(){
        scanIndexConfig();
    }

    public static IndexConfigRegistry getInstance(){
        return REGISTRY;
    }

    public List<Map<String, String>> getIndexConfigs() {
        return configs;
    }

    private void scanIndexConfig() {
        List<String> indexNames = findIndexNames();

        for (String indexName : indexNames) {
            Map<String, String> config = new HashMap<>();
            config.put("indexName", indexName);

            // 2 add child folder settings and mappings
            // TODO: validation: not found exception
            config.put("indexSettingsPath", INDEX_CONFIG_LOCATION + indexName + "/settings.json");
            config.put("indexMappingPath", INDEX_CONFIG_LOCATION + indexName + "/mapping.json");

            // 3 add sql folder's sql and sql.properties
            // TODO: validation: not found exception
            config.put("initSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/init.sql");
            config.put("syncSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/sync.sql");
            config.put("deleteSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/delete.sql");
            config.put("extensionSqlPath", INDEX_CONFIG_LOCATION + indexName + "/sql/extension.sql");

            // TODO: validation: not found exception
            String sqlPropertiesPath = INDEX_CONFIG_LOCATION + indexName + "/sql/sql.properties";
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

    private List<String> findIndexNames() {
        File indexConfigRootDir = null;
        try {
            final URL resource = getClass().getClassLoader().getResource(INDEX_CONFIG_LOCATION);
            if (resource == null) {
                throw new EspipeException("Root directory es is not found by " + INDEX_CONFIG_LOCATION);
            }
            indexConfigRootDir = new File(resource.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (indexConfigRootDir == null) {
            throw new EspipeException("Root directory es is not found by " + INDEX_CONFIG_LOCATION);
        }

        List<String> indexNames = new ArrayList<>();
        for (File indexConfigDir : indexConfigRootDir.listFiles()) {
            indexNames.add(indexConfigDir.getName());
        }
        return indexNames;
    }
}
