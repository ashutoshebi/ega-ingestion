drop table if exists PROCESS_DOWNLOAD_BOX_FILE;
drop table if exists HISTORIC_PROCESS_DOWNLOAD_BOX_FILE;

create table PROCESS_DOWNLOAD_BOX_FILE
(
    ID          varchar(255) primary key,
    INSTANCE_ID varchar(255) not null,
    RESULT_PATH text         not null,
    DOS_ID      text         not null,
    PASSWORD    text         not null,
    START_TIME  timestamp    not null
);

create table HISTORIC_PROCESS_DOWNLOAD_BOX_FILE
(
    ID                 bigserial primary key,
    MESSAGE_ID         varchar(255) not null,
    INSTANCE_ID        varchar(255) not null,
    RESULT_PATH        text         not null,
    DOS_ID             text         not null,
    FINISHED_CORRECTLY boolean      not null,
    MESSAGE            text,
    START_TIME         timestamp    not null,
    END_TIME           timestamp    not null
);
