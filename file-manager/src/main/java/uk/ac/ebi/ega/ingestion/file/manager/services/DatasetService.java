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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.ega.ingestion.file.manager.models.EgaFile;

import java.util.Collection;

import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileExtensionUtils.getFileExtension;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileExtensionUtils.removeEncryptionExtension;

public class DatasetService implements IDatasetService {

    private NamedParameterJdbcTemplate jdbcTemplate;

    public DatasetService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<EgaFile> getFiles(String datasetId) {
        String sql = "SELECT stable_id, file_name " +
                "FROM file " +
                "WHERE dataset_stable_id=:dataset_stableId";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("dataset_stableId", datasetId);
        return jdbcTemplate.query(sql, parameters, (resultSet, i) -> {
                    String dosId = normalizeFirePath(resultSet.getString("file_name"));
                    String fileExtension = removeEncryptionExtension(getFileExtension(dosId));
                    return new EgaFile(resultSet.getString("stable_id"), dosId, fileExtension);
                }
        );
    }

    private String normalizeFirePath(String file_name) {
        if (file_name.startsWith("/fire/A/ega/vol1/")) {
            return file_name.replaceFirst("/fire/A/ega/vol1/", "");
        }
        return file_name;
    }
}
