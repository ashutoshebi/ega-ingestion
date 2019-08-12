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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileDetailsRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@TestPropertySource(locations = "classpath:test.properties")
public class FileStatusUpdaterServiceIT {

    private static final Long FIRE_ID = 12L;

    // TODO bjuhasz: write meaningful tests which test the FileStatusUpdaterService class using this test
    @Autowired
    private FileHierarchyRepository fileHierarchyRepository;

    @Autowired
    private FileDetailsRepository fileDetailsRepository;

    private final IFireService fireService = mock(IFireService.class);

    @TestConfiguration
    @EnableJpaRepositories(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.repository"})
    @EntityScan(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.entities"})
    @EnableJpaAuditing
    static class Configuration {

    }

    @Test
    public void saveFileRootDirectory() {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "test1.bam", createFileDetails());
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findByOriginalPath("test1.bam");
        assertTrue(byOriginalPath.isPresent());
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void saveFileInDirectory() {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1.bam", createFileDetails());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findByOriginalPath("/test");
        assertTrue(byOriginalPath.isPresent());
        assertEquals(1, byOriginalPath.get().getChildPaths().size());
        TestTransaction.end();
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void saveFilesInDirectory() {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1.bam", createFileDetails());
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test2.bam", createFileDetails());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findByOriginalPath("/test");
        assertTrue(byOriginalPath.isPresent());
        assertEquals(2, byOriginalPath.get().getChildPaths().size());
        TestTransaction.end();
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void synchronizeFileStatusBetweenFireAndFileDetailsRepository() {
        final FileStatusUpdaterService fileStatusUpdaterService = new FileStatusUpdaterService(fileDetailsRepository, fireService);

        fileStatusUpdaterService.synchronizeFileStatusBetweenFireAndFileDetailsRepository();

        // TODO bjuhasz: write meaningful tests
    }


    private FileDetails createFileDetails() {
        return new FileDetails(
                "/box/ega-box-01/ega-box-01-012345677890.cip",
                26L,
                "3C130EA5D8D2D3DACA7F6808CDF0F148",
                42L,
                "3C130EA5D8D2D3DACA7F6808CDF0F149",
                "password",
                FileStatus.ARCHIVE_IN_PROGRESS,
                FIRE_ID
        );
    }

}
