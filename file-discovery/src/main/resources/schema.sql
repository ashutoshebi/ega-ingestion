drop table if exists STAGING_AREA_FILES;
drop table if exists STAGING_AREAS;

create table STAGING_AREAS
(
  ID             varchar(255) primary key,
  PATH           varchar(255) not null,
  ENABLED        boolean      not null,
  ACCOUNT        varchar(255) not null,
  POLLING_PERIOD integer      not null,
  FILES_PER_POLL integer      not null
);

create table STAGING_AREA_FILES
(
  ID              varchar(255) primary key,
  STAGING_AREA_ID varchar(255) not null,
  RELATIVE_PATH   text         not null,
  NAME            varchar(255) not null,
  SIZE            integer      not null,
  CREATE_DATE     timestamp    not null,
  UPDATE_DATE     timestamp    not null,
  CONSTRAINT FK_STAGING_AREA_FILE_TO_AREA_ID FOREIGN KEY (STAGING_AREA_ID) REFERENCES STAGING_AREAS (ID)
);