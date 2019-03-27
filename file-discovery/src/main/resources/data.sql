insert into STAGING_AREAS (ID, PATH, IGNORE_PATH_REGEX, DISCOVERY_ENABLED, INGESTION_ENABLED, ACCOUNT,
                           DISCOVERY_POLLING_PERIOD, INGESTION_POLLING_PERIOD, CREATE_DATE, UPDATE_DATE)
values ('box-1', '/tmp/ingestion/box-1', 'ega_metadata', true, false, 'USER-0', 3000, 3000,
        TIMESTAMP '2019-02-20 15:36:38', TIMESTAMP '2019-02-20 15:36:38');
insert into STAGING_AREAS (ID, PATH, IGNORE_PATH_REGEX, DISCOVERY_ENABLED, INGESTION_ENABLED, ACCOUNT,
                           DISCOVERY_POLLING_PERIOD, INGESTION_POLLING_PERIOD, CREATE_DATE, UPDATE_DATE)
values ('box-2', '/tmp/ingestion/box-2', 'ega_metadata', true, false, 'USER-0', 3000, 3000,
        TIMESTAMP '2019-02-20 15:36:38', TIMESTAMP '2019-02-20 15:36:38');
insert into STAGING_AREAS (ID, PATH, IGNORE_PATH_REGEX, DISCOVERY_ENABLED, INGESTION_ENABLED, ACCOUNT,
                           DISCOVERY_POLLING_PERIOD, INGESTION_POLLING_PERIOD, CREATE_DATE, UPDATE_DATE)
values ('box-3', '/tmp/ingestion/box-3', '', true, false, 'USER-1', 3000, 3000,
        TIMESTAMP '2019-02-20 15:36:38', TIMESTAMP '2019-02-20 15:36:38');
insert into STAGING_AREAS (ID, PATH, IGNORE_PATH_REGEX, DISCOVERY_ENABLED, INGESTION_ENABLED, ACCOUNT,
                           DISCOVERY_POLLING_PERIOD, INGESTION_POLLING_PERIOD, CREATE_DATE, UPDATE_DATE)
values ('box-4', '/tmp/ingestion/box-4', '', true, false, 'USER-2', 3000, 3000,
        TIMESTAMP '2019-02-20 15:36:38', TIMESTAMP '2019-02-20 15:36:38');