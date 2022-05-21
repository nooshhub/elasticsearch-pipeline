# elasticsearch-pipeline
Elasticsearch pipeline is a project for synchronizing data from database, also convert data first before we push data to elasticsearch, the current solutions from logstash does not help anymore, so we make our own.

### build
```bash
mvn clean install
```


### deploy and release
```bash
mvn clean deploy -P release
```

