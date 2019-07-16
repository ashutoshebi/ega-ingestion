drop table if exists JOB_EXECUTION;
drop table if exists JOB_RUN;
drop table if exists ENCRYPT_PARAMETERS;

create table JOB_EXECUTION
(
    JOB_ID      varchar(255) primary key,
    JOB_NAME    varchar(255)        not null,
    INSTANCE_ID varchar(255) unique not null,
    START_TIME  timestamp           not null
);

create table JOB_RUN
(
    ID          bigserial primary key,
    JOB_ID      varchar(255) not null,
    INSTANCE_ID varchar(255) not null,
    MESSAGE     text,
    START_TIME  timestamp    not null,
    END_TIME    timestamp    not null
);

create table ENCRYPT_PARAMETERS
(
    JOB_ID                varchar(255) not null,
    ACCOUNT_ID            varchar(255) not null,
    STAGING_ID            varchar(255) not null,
    GPG_PATH              text         not null,
    GPG_STAGING_PATH      text         not null,
    GPG_SIZE              bigint       not null,
    GPG_LAST_MODIFIED     bigint       not null,
    MD5_PATH              text         not null,
    MD5_STAGING_PATH      text         not null,
    MD5_SIZE              bigint       not null,
    MD5_LAST_MODIFIED     bigint       not null,
    GPG_MD5_PATH          text         not null,
    GPG_MD5_STAGING_PATH  text         not null,
    GPG_MD5_SIZE          bigint       not null,
    GPG_MD5_LAST_MODIFIED bigint       not null,
    RESULT_PATH           text         not null,
    CREATE_DATE           timestamp    not null
);