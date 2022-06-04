create database testdb;
use testdb;
create table nh_project (
    nh_project_id bigint AUTO_INCREMENT,
    name varchar (255),
    user_ids varchar (255),
    create_date timestamp ,
    mod_date timestamp,
    primary key(nh_project_id)
);

create table nh_property_attribute (
    nh_property_attribute_id bigint AUTO_INCREMENT,
    nh_project_id bigint,
    attribute_label varchar (255),
    attribute_key varchar (255),
    attribute_value varchar (255),
    create_date timestamp,
    mod_date timestamp,
    primary key(nh_property_attribute_id)
);

create table espipe_timer (
    index_name varchar(255),
    last_refresh_time timestamp,
    primary key(index_name)
)

