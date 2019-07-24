create schema FILE_INGESTION;

create table FILE_INGESTION.FILE_DETAILS (
    ID                       integer         identity   not null,
    CREATED_DATE             timestamp                  not null,
    ENCRYPTED_MD5            varchar(255)               not null,
    ENCRYPTED_SIZE           integer                    not null,
    END_DATE_TIME            timestamp                  not null,
    KEY_PATH                 varchar(255)               not null,
    PLAIN_MD5                varchar(255)               not null,
    PLAIN_SIZE               integer                    not null,
    STAGING_PATH             varchar(255)               not null,
    START_DATE_TIME          timestamp                  not null,
    STATUS                   varchar(255)               not null,
    UPDATE_DATE              timestamp                  not null,
    primary key (id)
);

create table FILE_INGESTION.FILE_HIERARCHY (
    ID              integer         identity not null,
    ACCOUNT_ID      varchar(255)             not null,
    CREATED_DATE    timestamp                not null,
    FILE_TYPE       varchar(255)             not null,
    NAME            varchar(255)             not null,
    ORIGINAL_PATH   varchar(4096)            not null,
    STAGING_AREA_ID varchar(255)             not null,
    UPDATE_DATE     timestamp                not null,
    FILE_DETAILS_ID integer,
    PARENT_ID       integer,
    primary key (id)
);

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
    RELATIVE_PATH   varchar(4096)not null,
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
    ID              int identity primary key,
    BOX_ID          varchar(255) not null,
    DATASET_ID      varchar(255) not null,
    TICKET_ID       varchar(255) not null,
    PASSWORD        varchar(255) not null,
    GENERATED_BY    varchar(255) not null,
    MAIL            BLOB         not null,
    PROCESSED_FILES int          not null,
    /*PROCESSED_FILES is only for query optimization, COUNT */
    TOTAL_FILES     int          not null,
    START_DATE      timestamp    not null,
    UPDATE_DATE     timestamp    not null,
    CONSTRAINT FK_DOWNLOAD_BOX_JOB_TO_DOWNLOAD_BOX_ASSIGNATION FOREIGN KEY (BOX_ID)
        REFERENCES DOWNLOAD_BOX_ASSIGNATION (BOX_ID)
);

create table DOWNLOAD_BOX_FILE_JOB
(
    ID             varchar(255) primary key,
    JOB_ID         bigint       not null,
    FILE_ID        varchar(255) not null,
    FILE_EXTENSION varchar(255) not null,
    DOS_ID         varchar(255) not null,
    STATUS         varchar(255) not null,
    ERROR_MESSAGE  BLOB,
    PROCESS_START  timestamp,
    PROCESS_END    timestamp,
    CONSTRAINT FK_DOWNLOAD_BOX_FILE_JOB_TO_DOWNLOAD_BOX_JOB FOREIGN KEY (JOB_ID) REFERENCES DOWNLOAD_BOX_JOB (ID)
);

create table HISTORIC_DOWNLOAD_BOX_JOB
(
    ID           bigint primary key,
    BOX_ID       varchar(255) not null,
    DATASET_ID   varchar(255) not null,
    TICKET_ID    varchar(255) not null,
    USER_ID      varchar(255) not null,
    BOX_PATH     varchar(255) not null,
    PASSWORD     varchar(255) not null,
    GENERATED_BY varchar(255) not null,
    START_DATE   timestamp    not null,
    END_DATE     timestamp    not null
);

create table HISTORIC_DOWNLOAD_BOX_FILE_JOB
(
    ID             varchar(255) primary key,
    JOB_ID         bigint       not null,
    FILE_ID        varchar(255) not null,
    FILE_EXTENSION varchar(4096)not null,
    DOS_ID         varchar(4096)not null,
    PROCESS_START  timestamp    not null,
    PROCESS_END    timestamp    not null,
    CONSTRAINT FK_HISTORIC_DOWNLOAD_BOX_FILE_JOB_TO_HISTORIC_DOWNLOAD_BOX_JOB FOREIGN KEY (JOB_ID)
        REFERENCES HISTORIC_DOWNLOAD_BOX_JOB (ID)
);



