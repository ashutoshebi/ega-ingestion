drop table if exists JOB_EXECUTION;
drop table if exists JOB_RUN;
drop table if exists RE_ENCRYPT_PARAMETERS;

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

create table RE_ENCRYPT_PARAMETERS
(
    JOB_ID             varchar(255) primary key,
    RESULT_PATH        text not null,
    DOS_ID             text not null,
    ENCRYPTED_PASSWORD text not null
)
