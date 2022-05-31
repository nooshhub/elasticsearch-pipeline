create table nh_project (
    nh_project_id identity,
    name varchar (255),
    create_date timestamp ,
    mod_date timestamp,
    primary key(nh_project_id)
);

create table nh_property_attribute (
    nh_property_attribute_id identity,
    nh_project_id number(18),
    attribute_label varchar (255),
    attribute_key varchar (255),
    attribute_value varchar (255),
    create_date timestamp,
    mod_date timestamp,
    primary key(nh_property_attribute_id)
);
