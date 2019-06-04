drop table if exists ukbiobank.result_statuses;
drop table if exists ukbiobank.re_encrypted_files;

create table ukbiobank.result_statuses
(
	result_status_id int2 primary key not null,
	result_status_value varchar(256) unique not null
);

-- alter table ukbiobank.result_statuses owner to ega;
-- grant all on table ukbiobank.result_statuses to ega;

create table ukbiobank.re_encrypted_files
(
	re_encrypted_file_id bigserial primary key,

	original_file_path varchar unique not null,
	new_re_encrypted_file_path varchar null,

	unencrypted_md5 varchar(256) not null,
	original_encrypted_md5 varchar(256) null,
	new_re_encrypted_md5 varchar(256) null,

	result_status_id smallint not null references ukbiobank.result_statuses(result_status_id),
	result_status_message varchar null,
	result_status_exception varchar null,
	start_time timestamp not null,
	end_time timestamp null
);


-- alter table ukbiobank.re_encrypted_files owner to ega;
-- grant all on table ukbiobank.re_encrypted_files to ega;

