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

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptComplete;
import uk.ac.ebi.ega.ingestion.file.manager.FileManagerApplication;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {FileManagerApplication.class})
public class FileManagerServiceTest {

    @Autowired
    private IFileManagerService fileManagerService;

    @ClassRule
    public static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1);

    @Test
    public void createFileHierarchy_CreateFileHierarchySuccessfully() throws FileHierarchyException {
        fileManagerService.createFileHierarchy(newEncryptComplete());
    }

    @Test
    public void findAll_WhenGivenFilePath_ThenReturnsAllFileTreeSuccessfully() throws FileHierarchyException {
        //Creates File Tree
        fileManagerService.createFileHierarchy(newEncryptComplete());

        //Retrieves File Tree
        List<FileHierarchy> fileHierarchyList = fileManagerService.findAll("/nfs/ega/public/box/ega-box-1130/test.gpg");

        assertNotNull(fileHierarchyList);
        assertEquals(1, fileHierarchyList.size());
        assertNotNull(fileHierarchyList.get(0));
        assertEquals("test.gpg", fileHierarchyList.get(0).getName());
        assertEquals("/nfs/ega/public/box/ega-box-1130/test.gpg", fileHierarchyList.get(0).getOriginalPath());

        //Retrieves File Tree
        fileHierarchyList = fileManagerService.findAll("/nfs/ega/public/box/ega-box-1130");

        assertNotNull(fileHierarchyList);
        assertNotNull(fileHierarchyList.get(0));
        assertEquals("test.gpg", fileHierarchyList.get(0).getName());
        assertEquals("/nfs/ega/public/box/ega-box-1130/test.gpg", fileHierarchyList.get(0).getOriginalPath());

        //Retrieves File Tree
        fileHierarchyList = fileManagerService.findAll("/nfs/ega/public/box");

        assertNotNull(fileHierarchyList);
        assertNotNull(fileHierarchyList.get(0));
        assertEquals("ega-box-1130", fileHierarchyList.get(0).getName());
        assertEquals("/nfs/ega/public/box/ega-box-1130", fileHierarchyList.get(0).getOriginalPath());
    }

    private EncryptComplete newEncryptComplete() {
        return new EncryptComplete("accountId",
                "stagingId",
                "/nfs/ega/public/box/ega-box-1130/test.gpg",
                "/nfs/ega/private/box/ega-box-1130/test.gpg",
                12345,
                "AHSBSHDHBD34HBHDD",
                12345,
                "OKFHRHDHBD78HBHDC",
                "/nfs/ega/private/box/ega-box-1130/key.txt",
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
