drop table if exists ukbiobank.re_encrypted_files;

-- alter table ukbiobank.result_statuses owner to ega;
-- grant all on table ukbiobank.result_statuses to ega;

create table ukbiobank.re_encrypted_files
(
    re_encrypted_file_id       bigserial primary key,

    original_file_path         varchar unique not null,
    new_re_encrypted_file_path varchar        null,

    unencrypted_md5            varchar(256)   null,
    original_encrypted_md5     varchar(256)   null,
    new_re_encrypted_md5       varchar(256)   null,

    unencrypted_size           bigint         null,
    fire_id                    bigint         null,

    result_status_message      text           null,
    result_status_exception    text           null,
    start_time                 timestamp      not null,
    end_time                   timestamp      null
);

-- alter table ukbiobank.re_encrypted_files owner to ega;
-- grant all on table ukbiobank.re_encrypted_files to ega;

