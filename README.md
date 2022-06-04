# elasticsearch-pipeline
Elasticsearch pipeline is a solution to replace logstash jdbc for synchronizing data from database.
The most important thing is Logstash is for a common standard usage, but our case is more complicated.
We had a one to many case, let's say you can a table with a extension table, the sql result will be N rows extension for just one standard row,
the aggregation on database side could get length limitation, the sync sql script just failed by this limitation.
Also Logstash keeps missing some data in synchronization we don't even know why.
So we make our own system to sync and track data. 

### build
```bash
mvn clean install
```


### deploy and release
```bash
mvn clean deploy -P release
```

