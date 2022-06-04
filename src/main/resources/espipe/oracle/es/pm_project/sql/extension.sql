select pm.param_name,
coalesce(ppa.string_value, to_char(ppa.date_value, 'YYYY-MM-DD'), to_char(ppa.number_value))  attribute_value
from PR_PROPERTY_ATTRIBUTE ppa, pr_property_param pm where ppa.pr_property_param_id = pm.pr_property_param_id and ppa.PR_PROPERTY_ID = ?
