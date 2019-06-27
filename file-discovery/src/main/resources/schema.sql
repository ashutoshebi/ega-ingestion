drop table if exists STAGING_AREA_FILES;
drop table if exists STAGING_AREAS;

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
    FILE_SIZE       bigint      not null,
    UPDATE_DATE     timestamp    not null,
    CONSTRAINT FK_STAGING_AREA_FILE_TO_AREA_ID FOREIGN KEY (STAGING_AREA_ID) REFERENCES STAGING_AREAS (ID)
);