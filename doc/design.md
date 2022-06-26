### design

![Architecture](https://mmbiz.qpic.cn/mmbiz_png/RGINbiaOhjW9feEzaNvCSyUEUEuNwArQEdgWtSMwWftNLxqNkojHadep9DBOJng9ictxff33PEHz5DXob2iansWfw/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1)

![N TO 1](https://mmbiz.qpic.cn/mmbiz_png/RGINbiaOhjW9feEzaNvCSyUEUEuNwArQECPapibVmdDhlYeLdDpyVDia1FOviau2JQibH8ZWVArJAm2hOVBJD4jV69g/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1)

- case 1: aggregate fields to one field was failed, since sql field has length limit
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
we will log the error
- case 4: speed
currently 100k could be synced in 15s, I believe we could make this much faster since load 100k data from a database only takes 4s.


