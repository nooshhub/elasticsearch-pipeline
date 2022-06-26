select
pp.pm_project_id as pm_project_id, pp.reference, pp.project_number, pp.name, pp.owner_ac_workgroup_id, pp.oc_object_state_id, pp.CUSTOM_PR_PROPERTY_ID,
to_char(pp.mod_date, 'YYYY-MM-DD"T"HH24:MI:SS".000Z"') mod_date, to_char(pp.create_date, 'YYYY-MM-DD"T"HH24:MI:SS".000Z"') create_date,
pps.description, pps.comments,
pm_member.user_ids
from
  pm_project pp,
  pm_project_sd pps,
  (select tm_team_object.object_id, listagg(tm_team_member.user_id, ',') WITHIN GROUP (order by null) as user_ids from tm_team_member, tm_team_object
  where
  tm_team_object.oc_object_class_id = 1000000 and
  tm_team_member.tm_team_id = tm_team_object.tm_team_id and
  tm_team_member.current_record = 1
  group by tm_team_object.object_id) pm_member
where pp.pm_project_id = pm_member.object_id
and pp.pm_project_id = pps.pm_project_id
and nvl(pp.mod_date, pp.create_date) < ?

