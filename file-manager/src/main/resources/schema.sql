begin transaction;
drop table if exists STAGING_AREA_FILES cascade;
drop table if exists STAGING_AREAS cascade;
drop table if exists HISTORIC_DOWNLOAD_BOX_FILE_JOB cascade;
drop table if exists DOWNLOAD_BOX_FILE_JOB cascade;
drop table if exists HISTORIC_DOWNLOAD_BOX_JOB cascade;
drop table if exists DOWNLOAD_BOX_JOB cascade;
drop table if exists HISTORIC_DOWNLOAD_BOX_ASSIGNATION cascade;
drop table if exists DOWNLOAD_BOX_ASSIGNATION cascade;
drop table if exists DOWNLOAD_BOX cascade;
end transaction;

create table STAGING_AREAS
(
    ID                       varchar(255) primary key,
    PATH                     varchar(255) not null unique,
    IGNORE_PATH_REGEX        varchar(255) not null,
    DISCOVERY_ENABLED        boolean      not null,
    INGESTION_ENABLED        boolean      not null,
    ACCOUNT                  varchar(255) not null,
    DISCOVERY_POLLING_PERIOD integer      not null,
    INGESTION_POLLING_PERIOD integer      not null,
    CREATE_DATE              timestamp    not null,
    UPDATE_DATE              timestamp    not null
);

create table STAGING_AREA_FILES
(
    ID              varchar(255) primary key,
    STAGING_AREA_ID varchar(255) not null,
    RELATIVE_PATH   text         not null,
    FILE_SIZE       integer      not null,
    UPDATE_DATE     timestamp    not null,
    CONSTRAINT FK_STAGING_AREA_FILE_TO_AREA FOREIGN KEY (STAGING_AREA_ID) REFERENCES STAGING_AREAS (ID)
);

create table DOWNLOAD_BOX
(
    BOX_NAME    varchar(255) primary key,
    BOX_PATH    varchar(255) not null unique,
    CREATE_DATE timestamp    not null,
    UPDATE_DATE timestamp    not null
);

create table DOWNLOAD_BOX_ASSIGNATION
(
    BOX_ID      varchar(255) primary key,
    USER_ID     varchar(255) not null unique,
    ASSIGNED_BY varchar(255) not null,
    FROM_DATE   timestamp    not null,
    UNTIL_DATE  timestamp    not null,
    UPDATE_DATE timestamp,
    CONSTRAINT FK_DOWNLOAD_BOX_ASSIGNATION_TO_DOWNLOAD_BOX FOREIGN KEY (BOX_ID) REFERENCES DOWNLOAD_BOX (BOX_NAME)
);

create table HISTORIC_DOWNLOAD_BOX_ASSIGNATION
(
    BOX_ID      varchar(255) not null,
    USER_ID     varchar(255) not null,
    ASSIGNED_BY varchar(255) not null,
    FROM_DATE   timestamp    not null,
    UNTIL_DATE  timestamp    not null
);

create table DOWNLOAD_BOX_JOB
(
    ID           bigserial primary key,
    BOX_ID       varchar(255) not null,
    DATASET_ID   varchar(255) not null,
    TICKET_ID    varchar(255) not null,
    PASSWORD     varchar(255) not null,
    GENERATED_BY varchar(255) not null,
    START_DATE   timestamp    not null,
    CONSTRAINT FK_DOWNLOAD_BOX_JOB_TO_DOWNLOAD_BOX_ASSIGNATION FOREIGN KEY (BOX_ID)
        REFERENCES DOWNLOAD_BOX_ASSIGNATION (BOX_ID)
);

create table DOWNLOAD_BOX_FILE_JOB
(
    ID            bigserial primary key,
    JOB_ID        bigserial    not null,
    FILE          text         not null,
    INSTANCE_ID   varchar(255) not null,
    PROCESS_START timestamp,
    PROCESS_END   timestamp,
    CONSTRAINT FK_DOWNLOAD_BOX_FILE_JOB_TO_DOWNLOAD_BOX_JOB FOREIGN KEY (JOB_ID) REFERENCES DOWNLOAD_BOX_JOB (ID)
);

create table HISTORIC_DOWNLOAD_BOX_JOB
(
    ID           bigint primary key,
    DATASET_ID   varchar(255) not null,
    BOX_ID       varchar(255) not null,
    BOX_PATH     varchar(255) not null,
    REQUESTED_BY varchar(255) not null,
    GENERATED_BY varchar(255) not null,
    PASSWORD     varchar(255) not null,
    START_DATE   timestamp    not null,
    END_DATE     timestamp    not null
);

create table HISTORIC_DOWNLOAD_BOX_FILE_JOB
(
    ID            bigserial primary key,
    JOB_ID        bigserial    not null,
    FILE          text         not null,
    INSTANCE_ID   varchar(255) not null,
    PROCESS_START timestamp,
    PROCESS_END   timestamp,
    CONSTRAINT FK_HISTORIC_DOWNLOAD_BOX_FILE_JOB_TO_HISTORIC_DOWNLOAD_BOX_JOB FOREIGN KEY (JOB_ID)
        REFERENCES DOWNLOAD_BOX_JOB (ID)
);