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


### Contribute
- Please feel free to submit an issue or a PR. 
- [contribute guide](/doc/contribute_guide.md)
- [design](/doc/design.md)
- [tbd and release notes](/doc/releases.md)

### deploy and release
```bash 
# run mvn_prepare_release.cmd before release, this will check code style
mvn clean deploy -P release
```

### Sponsor

Dora, Kevin, Lanceadd, MrsMeow