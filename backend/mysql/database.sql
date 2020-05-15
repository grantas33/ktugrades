drop table if exists Mark;
drop table if exists MarkInformation;
drop table if exists MarkSlot;
drop table if exists Module;
drop table if exists UserSubscriptions;
drop table if exists User;

create table User
(
    username binary(16) not null,
    password binary(16) not null,
    constraint User_pk primary key (username)
) ROW_FORMAT=DYNAMIC character set utf8mb4;

create table UserSubscriptions
(
    id binary(16) not null,
    userId binary(16) not null,
    endpoint varchar(255) not null,
    publicKey varchar(255) not null,
    auth varchar(255) not null,
    constraint UserSubscriptions_pk primary key (id),
    constraint UserSubscriptions_User_username_fk foreign key (userId) references User (username) on delete cascade,
    unique key UserSubscriptions_endpoint_publicKey_auth_uindex (endpoint, publicKey, auth)
) ROW_FORMAT=DYNAMIC character set utf8mb4;

create table Module
(
    code varchar(255) not null,
    semesterCode varchar(255) not null,
    title varchar(255) null,
    professor varchar(255) null,
    constraint Module_pk
        primary key (code, semesterCode)
) ROW_FORMAT=DYNAMIC character set utf8mb4;

create table MarkSlot
(
    moduleCode varchar(255) not null,
    semesterCode varchar(255) not null,
    typeId varchar(32) null,
    week varchar(32) not null,
    averageMark double null,
    constraint MarkSlot_pk
        primary key (moduleCode, semesterCode, typeId, week),
    constraint MarkSlot_Module_code_semesterNumber_fk
        foreign key (moduleCode, semesterCode) references Module (code, semesterCode)
) ROW_FORMAT=DYNAMIC character set utf8mb4;

create table MarkInformation
(
    id binary(16) not null,
    moduleCode varchar(255) not null,
    semesterCode varchar(255) not null,
    userId binary(16) not null,
    typeId varchar(32) null,
    week varchar(32) not null,
    date datetime not null,
    constraint MarkInformation_pk
        primary key (id),
    constraint MarkInformation_MarkSlot_moduleCode_semesterCode_typeId_week_fk
        foreign key (moduleCode, semesterCode, typeId, week) references MarkSlot (moduleCode, semesterCode, typeId, week),
    constraint MarkInformation_User_username_fk
        foreign key (userId) references User (username) on delete cascade
) ROW_FORMAT=DYNAMIC character set utf8mb4;

create table Mark
(
    id binary(16) not null,
    markInformationId binary(16) not null,
    mark varchar(32) not null,
    constraint Mark_pk
        primary key (id),
    constraint Mark_MarkInformation_id_fk
        foreign key (markInformationId) references MarkInformation (id) on delete cascade
) ROW_FORMAT=DYNAMIC character set utf8mb4
