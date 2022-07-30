# elasticsearch-pipeline
Elasticsearch pipeline is a solution to replace logstash jdbc for synchronizing data from database.
Support Elasticsearch version >= 7.17.4, current target version is 8.2.2.
[download](https://www.elastic.co/downloads/elasticsearch)

### get started
Start service.
```
cd espipe-core 
start_es_server.cmd
mvn_run.cmd
```  
Access API by postman.    
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
```bash
# run this to skip code style check, sicne we don't want any rules to slow our developing speed.
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