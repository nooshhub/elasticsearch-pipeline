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
based on this [discussion](https://discuss.elastic.co/t/approaches-to-deal-with-limit-of-total-fields-1000-in-index-has-been-exceeded/241039) we 
provide a flag custom_in_one
```yaml
espipe:
  elasticsearch:
    # fields_mode: flatten, all_in_one, custom_in_one
    fields_mode: custom_in_one
``` 
to use one field for all custom fields, let's call it custom_fields.

- case 3: data is missing by no reason while using logstash jdbc     
we will add log for each document 

### build
```bash
mvn clean install
```

### deploy and release
```bash
mvn clean deploy -P release
```

