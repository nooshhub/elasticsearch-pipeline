# elasticsearch-pipeline
Elasticsearch pipeline is a solution to replace logstash jdbc for synchronizing data from database.

- case 1: aggregate fields to one field, since sql field has length limit
```
error: ORA-01489: result of string concatenation is too long
```
- case 2: elasticsearch total fields has size limit
```
co.elastic.clients.elasticsearch._types.ElasticsearchException: 
[es/index] failed: [illegal_argument_exception] Limit of total fields [1000] has been exceeded
```
- case 3: data is missing by no reason while using logstash jdbc

### build
```bash
mvn clean install
```

### deploy and release
```bash
mvn clean deploy -P release
```

