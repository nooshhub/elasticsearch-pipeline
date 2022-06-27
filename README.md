# elasticsearch-pipeline
Elasticsearch pipeline is a solution to replace logstash jdbc for synchronizing data from database.
Support Elasticsearch version >= 7.17.4, current target version is 8.2.2.
[download](https://www.elastic.co/downloads/elasticsearch)

### get started
```
API is UNDER CONSTRUCTION, in version 0.0.3-SNAPSHOT   
Please USE UNIT TEST TO HAVE A TRY
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

- [design](/doc/design.md)
- [tbd and release notes](/doc/releases.md)

### Sponsor

Dora, Kevin, Lanceadd, MrsMeow