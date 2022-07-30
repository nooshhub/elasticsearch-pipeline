# elasticsearch-pipeline
Elasticsearch pipeline is a solution to replace logstash jdbc for synchronizing data from database.
Support Elasticsearch version >= 7.17.4, current target version is 8.2.2.
[download](https://www.elastic.co/downloads/elasticsearch)

### get started
Start service.
```
cd espipe-core 
// start your Elasticsearch 
start_es_server.cmd
// start espipe core
mvn_run.cmd
```  
Access API by postman or Apifox.    
Postman script is under espipe-core/src/test/postman/espipe.postman_collection.json.    
Import it into your postman.    
```
http://localhost:8713/please/start/init/all
http://localhost:8713/please/start/init/{indexName}
http://localhost:8713/please/stop/init/all
http://localhost:8713/please/stop/init/{indexName}
http://localhost:8713/please/start/sync/all
http://localhost:8713/please/start/sync/{indexName}
http://localhost:8713/please/stop/sync/all
http://localhost:8713/please/stop/sync/{indexName}
http://localhost:8713/please/fix/{indexName}/{id}
http://localhost:8713/please/show/metrics   
``` 

### develop
#### set up
1. please add this to your maven settings.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <!-- China only -->
    <mirrors>
        <mirror>
            <id>alimaven</id>
            <name>aliyun maven</name>
            <url>https://maven.aliyun.com/repository/central</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
    <!-- a necessity for spring-javaformat plugin-->
	<pluginGroups>
		<pluginGroup>io.spring.javaformat</pluginGroup>
	</pluginGroups>
</settings>
```

```bash
# run this to skip code style check, since we don't want any rules to slow our developing speed.
mvn_clean_install.cmd
```

### deploy and release
```bash 
# run mvn_prepare_release.cmd before release, this will check code style
mvn clean deploy -P release
```

### Contribute
- go ahead send us an issue to ask anything or PR to main branch.
- [design](/doc/design.md)
- [tbd and release notes](/doc/releases.md)

### Sponsor

Dora, Kevin, Lanceadd, MrsMeow