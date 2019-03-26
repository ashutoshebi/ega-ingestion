drop table if exists FILE_ENCRYPTION_JOB;
drop table if exists FILE_ENCRYPTION_LOG_SUCCESSFUL;
drop table if exists FILE_ENCRYPTION_LOG_FAILED;

create table FILE_ENCRYPTION_JOB
(
  ID              bigserial primary key,
  INSTANCE_ID     varchar(255) not null,
  ACCOUNT         varchar(255) not null,
  STAGING_AREA_ID varchar(255) not null,
  FILE_PATH       text         not null,
  CREATE_DATE     timestamp    not null,
  UNIQUE (INSTANCE_ID, ACCOUNT, STAGING_AREA_ID, FILE_PATH)
);

create table FILE_ENCRYPTION_LOG_SUCCESSFUL
(
  ID              bigint primary key,
  INSTANCE_ID     varchar(255) not null,
  ACCOUNT         varchar(255) not null,
  STAGING_AREA_ID varchar(255) not null,
  FILE_PATH       text         not null,
  MD5             varchar(255) not null,
  ENCRYPTED_MD5   varchar(255) not null,
  PROCESS_START   timestamp    not null,
  PROCESS_END     timestamp    not null
);

create table FILE_ENCRYPTION_LOG_FAILED
(
  ID              bigint primary key,
  INSTANCE_ID     varchar(255) not null,
  ACCOUNT         varchar(255) not null,
  STAGING_AREA_ID varchar(255) not null,
  FILE_PATH       text         not null,
  PROCESS_START   timestamp    not null,
  PROCESS_END     timestamp    not null,
  ERROR_MESSAGE   text
);