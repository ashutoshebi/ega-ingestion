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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.ega.ingestion.file.manager.models.EgaFile;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class DatasetServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatasetService dataService;

    /**
     * To test getFiles executes successfully, and return Collection of EgaFile when correct DataSetId given.
     */
    @Test
    public void getFiles_CorrectDataSetIdGiven_ShouldReturnFiles() {

        final List<EgaFile> egaFilesActual = Collections.EMPTY_LIST;

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(egaFilesActual);

        final Collection<EgaFile> egaFilesReceived = dataService.getFiles("datasetId_test");

        assertTrue(egaFilesActual == egaFilesReceived);
    }
}
