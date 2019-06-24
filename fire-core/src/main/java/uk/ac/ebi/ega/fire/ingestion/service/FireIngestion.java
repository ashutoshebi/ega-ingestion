/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.fire.ingestion.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import uk.ac.ebi.ega.fire.ingestion.model.FireIngestionModel;

import java.time.LocalDateTime;

public class FireIngestion implements IFireIngestion {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FireIngestion(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void ingest(final FireIngestionModel fireIngestionModel) {

        // Add entries into FIRE database

        insertRecordIntoFileTable(fireIngestionModel);
        insertRecordIntoArchiveTable(fireIngestionModel);
    }

    /**
     * @return long Database Generated key
     * @See https://github.com/EbiEga/ega-production/blob/master/database-commons/src/main/java/uk/ac/ebi/ega/database/commons/services/ProFilerService.java
     */
    private long insertRecordIntoFileTable(final FireIngestionModel fireIngestionModel) {
        final String insertFileSQL = getInsertIntoFileSQL();
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        final KeyHolder keyHolder = new GeneratedKeyHolder();

        setMappingsForFile(fireIngestionModel, parameters);
        jdbcTemplate.update(insertFileSQL, parameters, keyHolder);

        final Number number = keyHolder.getKey(); //Returns newly generated key. E.g. table has autoincrement primary key

        return number.longValue();
    }

    /**
     * @return long Database Generated key
     * @See https://github.com/EbiEga/ega-production/blob/master/database-commons/src/main/java/uk/ac/ebi/ega/database/commons/services/ProFilerService.java
     */
    private long insertRecordIntoArchiveTable(final FireIngestionModel fireIngestionModel) {
        final String insertArchiveSQL = getInsertIntoArchiveSQL();
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        final KeyHolder keyHolder = new GeneratedKeyHolder();

        setMappingsForArchive(fireIngestionModel, parameters);
        jdbcTemplate.update(insertArchiveSQL, parameters, keyHolder);

        final Number number = keyHolder.getKey(); // Returns newly generated key. E.g. table has autoincrement primary key

        return number.longValue();
    }

    private void setMappingsForFile(final FireIngestionModel source, final MapSqlParameterSource destination) {

        final LocalDateTime localDateTime = LocalDateTime.now();

        destination.addValue("name", source.getFileName());
        destination.addValue("md5", source.getEncryptedMd5());
        destination.addValue("type", source.getFileType());
        destination.addValue("size", source.getEncryptedSize());
        destination.addValue("host_id", 1);
        destination.addValue("created", localDateTime);
        destination.addValue("updated", localDateTime);
        destination.addValue("ega_id", "egaFileId"); //TODO need to check how to get. Replace with actual value.
    }

    private void setMappingsForArchive(final FireIngestionModel source, final MapSqlParameterSource destination) {

        final LocalDateTime localDateTime = LocalDateTime.now();

        destination.addValue("name", source.getFileName());
        destination.addValue("file_id", "fileId"); //TODO need to check how to get. Replace with actual value.
        destination.addValue("md5", source.getEncryptedMd5());
        destination.addValue("size", source.getEncryptedSize());
        destination.addValue("relative_path", "relativePath"); //TODO need to check how to get. Replace with actual value.
        destination.addValue("volume_name", "vol1");
        destination.addValue("priority", "50");
        destination.addValue("created", localDateTime);
        destination.addValue("updated", localDateTime);
        destination.addValue("archive_action_id", 1);
        destination.addValue("archive_location_id", 1);
    }

    private String getInsertIntoFileSQL() {
        return "INSERT INTO file(" +
                "name," +
                "md5," +
                "type," +
                "size," +
                "host_id," +
                "created," +
                "updated," +
                "ega_file_stable_id" +
                ") " +
                "VALUES(" +
                ":name," +
                ":md5," +
                ":type," +
                ":size," +
                ":host_id," +
                ":created," +
                ":updated," +
                ":ega_id)";
    }

    private String getInsertIntoArchiveSQL() {
        return "INSERT INTO archive(" +
                "name," +
                "file_id," +
                "md5," +
                "size," +
                "relative_path," +
                "volume_name," +
                "priority," +
                "created," +
                "updated," +
                "archive_action_id," +
                "archive_location_id" +
                ") " +
                "VALUES(" +
                ":name," +
                ":file_id," +
                ":md5," +
                ":size," +
                ":relative_path," +
                ":volume_name," +
                ":priority," +
                ":created," +
                ":updated," +
                ":archive_action_id," +
                ":archive_location_id)";
    }
}
