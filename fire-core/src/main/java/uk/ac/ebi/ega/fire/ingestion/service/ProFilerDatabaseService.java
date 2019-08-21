/*
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
 */
package uk.ac.ebi.ega.fire.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import uk.ac.ebi.ega.fire.utils.FileUtils;

import java.io.File;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

public class ProFilerDatabaseService implements IProFilerDatabaseService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProFilerDatabaseService.class);

    private final NamedParameterJdbcTemplate proFilerTemplate;

    @Autowired
    public ProFilerDatabaseService(NamedParameterJdbcTemplate proFilerTemplate) {
        this.proFilerTemplate = proFilerTemplate;
    }

    @Override
    public long archiveFile(String egaFileId, File file, String md5, String pathOnFire) {
        LOGGER.debug("Started archiving the {} file with the following parameters: " +
                "egaFileId: {}, md5: {}, pathOnFire: {}", file, egaFileId, md5, pathOnFire);

        long profilerFileId = insertFile(egaFileId, file, md5);
        LOGGER.debug("File has been inserted into the FILE table. " +
                        "profilerFileId: {}, egaFileId: {}, file: {} ", profilerFileId, egaFileId, file);

        final long archiveId = insertArchive(profilerFileId, pathOnFire, file, md5);
        LOGGER.debug("File has been inserted into the ARCHIVE table. " +
                "archiveId: {}, profilerFileId: {}, pathOnFire: {}", archiveId, profilerFileId, pathOnFire);

        return archiveId;
    }

    @Override
    public List<OldFireFile> findAllByFireId(final List<Long> fireIds) {
        final String query = "SELECT " +
                "archive_id, " +
                "fire_exit_code, " +
                "fire_exit_reason " +
                "FROM archive " +
                "WHERE archive_id in (:fire_ids)";

        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("fire_ids", fireIds);

        final RowMapper<OldFireFile> rowMapper = (rs, rowNum) -> {
            final Long fireId = rs.getLong("archive_id");
            final Integer exitCode = rs.getInt("fire_exit_code");
            final String exitReason = rs.getString("fire_exit_reason");
            return new OldFireFile(fireId, exitCode, exitReason);
        };

        return proFilerTemplate.query(query, parameters, rowMapper);
    }

    private long insertFile(String egaFileId, File file, String md5) {
        String query = "INSERT INTO file(" +
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
        Timestamp date = getCurrentDate();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("name", file.getName());
        parameters.addValue("md5", md5);
        parameters.addValue("type", FileUtils.getType(file.getName()));
        parameters.addValue("size", file.length());
        parameters.addValue("host_id", 1);
        parameters.addValue("created", date);
        parameters.addValue("updated", date);
        parameters.addValue("ega_id", egaFileId);

        KeyHolder holder = new GeneratedKeyHolder();
        proFilerTemplate.update(query, parameters, holder);
        return holder.getKey().longValue();
    }

    private long insertArchive(Number fileId, String relativePath, File file, String md5) {
        String query = "INSERT INTO archive(" +
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
        Timestamp date = getCurrentDate();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("name", file.getName());
        parameters.addValue("file_id", fileId);
        parameters.addValue("md5", md5);
        parameters.addValue("size", file.length());
        parameters.addValue("relative_path", relativePath);
        parameters.addValue("volume_name", "vol1");
        parameters.addValue("priority", "50");
        parameters.addValue("created", date);
        parameters.addValue("updated", date);
        parameters.addValue("archive_action_id", 1);
        parameters.addValue("archive_location_id", 1);

        KeyHolder holder = new GeneratedKeyHolder();
        proFilerTemplate.update(query, parameters, holder);
        // That's the archive_id:
        return holder.getKey().longValue();
    }

    private Timestamp getCurrentDate() {
        return new Timestamp(Calendar.getInstance().toInstant().toEpochMilli());
    }

}
