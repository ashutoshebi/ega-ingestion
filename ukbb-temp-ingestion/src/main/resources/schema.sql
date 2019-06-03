drop table if exists ukbiobank.result_statuses;
drop table if exists ukbiobank.re_encrypted_files;

CREATE TABLE ukbiobank.result_statuses
(
	result_status_id int2 NOT NULL,
	result_status_value varchar(256) NOT NULL,
	CONSTRAINT result_statuses_pkey PRIMARY KEY (result_status_id),
	CONSTRAINT result_statuses_result_status_value_key UNIQUE (result_status_value)
);

-- ALTER TABLE ukbiobank.result_statuses OWNER TO ega;
-- GRANT ALL ON TABLE ukbiobank.result_statuses TO ega;

CREATE TABLE ukbiobank.re_encrypted_files
(
	re_encrypted_file_id PRIMARY KEY,

	original_file_path varchar UNIQUE NOT NULL REFERENCES ukbiobank.files(file_path),
	new_re_encrypted_file_path varchar UNIQUE NULL,

	unencrypted_md5 varchar(256) UNIQUE NOT NULL REFERENCES ukbiobank.files(md5_checksum),
	original_encrypted_md5 varchar(256) UNIQUE NULL,
	new_re_encrypted_md5 varchar(256) UNIQUE NULL,

	result_status_id smallint NOT NULL REFERENCES ukbiobank.result_statuses(result_status_id),
	result_status_message varchar NULL,
	result_status_exception varchar NULL,
	start_time timestamp NOT NULL,
	end_time timestamp NULL
);

-- ALTER TABLE ukbiobank.re_encrypted_files OWNER TO ega;
-- GRANT ALL ON TABLE ukbiobank.re_encrypted_files TO ega;

