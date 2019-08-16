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
package uk.ac.ebi.ega.ingestion.file.manager.persistence.repository;

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
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@TestPropertySource(locations = "classpath:test.properties")
public class FileHierarchyRepositoryTest {

    @Autowired
    private FileHierarchyRepository fileHierarchyRepository;

    @TestConfiguration
    @EnableJpaRepositories(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.repository"})
    @EntityScan(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.entities"})
    @EnableJpaAuditing
    static class Configuration {

    }

    @Test
    public void saveFileRootDirectory() throws FileHierarchyException {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "test1.bam", createFileDetails());
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findOne("/test1.bam", "ega-account-01", "ega-staging-01");
        assertTrue(byOriginalPath.isPresent());
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void saveFileInDirectory() throws FileHierarchyException {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1.bam", createFileDetails());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findOne("/test", "ega-account-01", "ega-staging-01");
        assertTrue(byOriginalPath.isPresent());
        assertEquals(1, byOriginalPath.get().getChildPaths().size());
        TestTransaction.end();
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void saveFilesInDirectory() throws FileHierarchyException {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1.bam", createFileDetails());
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test2.bam", createFileDetails());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findOne("/test", "ega-account-01", "ega-staging-01");
        assertTrue(byOriginalPath.isPresent());
        assertEquals(2, byOriginalPath.get().getChildPaths().size());
        TestTransaction.end();
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void saveFilesInDirectoryAndRetrieveFolderWithNoChildren() throws FileHierarchyException {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1.bam", createFileDetails());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Optional<FileHierarchy> byOriginalPath = fileHierarchyRepository.findOne("/test/test1.bam", "ega-account-01", "ega-staging-01");
        assertTrue(byOriginalPath.isPresent());
        TestTransaction.end();

        TestTransaction.start();
        TestTransaction.flagForCommit();
        final FileHierarchy fileHierarchy = byOriginalPath.get();
        fileHierarchyRepository.deleteById(fileHierarchy.getId());
        TestTransaction.end();

        TestTransaction.start();
        final Optional<FileHierarchy> byOriginalPathAfterDeleted = fileHierarchyRepository.findOne("/test", "ega-account-01", "ega-staging-01");
        assertTrue(byOriginalPathAfterDeleted.isPresent());
        final FileHierarchy fileHierarchyAfterChildDeleted = byOriginalPathAfterDeleted.get();
        assertNotNull(fileHierarchyAfterChildDeleted.getChildPaths());
        assertTrue(fileHierarchyAfterChildDeleted.getChildPaths().isEmpty());
        TestTransaction.end();
    }


    /**
     * When pass valid AccountId, StagingAreaId & File as FileStructureType
     * returns FileHierarchy Stream object.
     */
    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenPassValidArgumentValues_ThenReturnsFileHierarchyAsStream() throws FileHierarchyException {
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1.bam", createFileDetails());
        fileHierarchyRepository.saveNewFile("ega-account-01", "ega-staging-01", "/test/test1/test2.bam", createFileDetails());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchy> fileHierarchyStream = fileHierarchyRepository.findAllFilesOrFoldersInRootPathRecursive("ega-account-01", "ega-staging-01", FileStructureType.FILE);
        assertNotNull(fileHierarchyStream);

        final Object[] fileHierarchyArray = fileHierarchyStream.toArray();

        assertEquals(2, fileHierarchyArray.length);

        final FileHierarchy fileHierarchyFirstValue = (FileHierarchy) fileHierarchyArray[0];
        assertEquals("ega-account-01", fileHierarchyFirstValue.getAccountId());
        assertEquals("ega-staging-01", fileHierarchyFirstValue.getStagingAreaId());
        assertEquals("/test/test1.bam", fileHierarchyFirstValue.getOriginalPath());

        final FileHierarchy fileHierarchySecondValue = (FileHierarchy) fileHierarchyArray[1];
        assertEquals("ega-account-01", fileHierarchySecondValue.getAccountId());
        assertEquals("ega-staging-01", fileHierarchySecondValue.getStagingAreaId());
        assertEquals("/test/test1/test2.bam", fileHierarchySecondValue.getOriginalPath());
        TestTransaction.end();
    }

    /**
     * When pass Invalid AccountId, StagingAreaId & File as FileStructureType
     * returns Empty Stream object.
     */
    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Transactional
    @Test
    public void findAll_WhenPassInValidArgumentValues_ThenReturnsEmptyStream() throws FileHierarchyException {
        final Stream<FileHierarchy> fileHierarchyStream = fileHierarchyRepository.findAllFilesOrFoldersInRootPathRecursive("ega-account-01", "ega-staging-01",
                FileStructureType.FILE);
        assertNotNull(fileHierarchyStream);
        assertEquals(0, fileHierarchyStream.count());
    }

    private FileDetails createFileDetails() {
        return new FileDetails(
                "/box/ega-box-01/ega-box-01-012345677890.cip",
                26L,
                "3C130EA5D8D2D3DACA7F6808CDF0F148",
                42L,
                "3C130EA5D8D2D3DACA7F6808CDF0F149",
                "password",
                "archiving"
        );
    }

}
