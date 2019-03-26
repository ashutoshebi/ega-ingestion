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
package uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecursiveFileVisitorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testVisitorNoFilter() throws IOException {
        temporaryFolder.newFile("test1.txt");
        temporaryFolder.newFile("test2.txt");
        temporaryFolder.newFile(".hidden_file.txt");
        temporaryFolder.newFolder("folder-1");
        temporaryFolder.newFolder(".hidden");
        temporaryFolder.newFile("/folder-1/test3.txt");
        temporaryFolder.newFile("/.hidden/test4.txt");

        RecursiveFileVisitor visitor = new RecursiveFileVisitor();
        Files.walkFileTree(temporaryFolder.getRoot().toPath(), new HashSet<>(), Integer.MAX_VALUE, visitor);
        final Map<String, FileStatic> fileSystemView = visitor.getFiles();
        assertEquals(5, fileSystemView.keySet().size());
        assertTrue(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/test1.txt"));
        assertTrue(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/.hidden_file.txt"));
        assertTrue(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/folder-1/test3.txt"));
        assertTrue(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/.hidden/test4.txt"));
        assertFalse(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/.hidden"));
    }

    @Test
    public void testVisitorHiddenFilter() throws IOException {
        temporaryFolder.newFile("test1.txt");
        temporaryFolder.newFile("test2.txt");
        temporaryFolder.newFile(".hidden_file.txt");
        temporaryFolder.newFolder("folder-1");
        temporaryFolder.newFolder(".hidden");
        temporaryFolder.newFile("/folder-1/test3.txt");
        temporaryFolder.newFile("/.hidden/test4.txt");

        RecursiveFileVisitor visitor = new RecursiveFileVisitor(new IgnoreHiddenFileListFilter());
        Files.walkFileTree(temporaryFolder.getRoot().toPath(), new HashSet<>(), Integer.MAX_VALUE, visitor);
        final Map<String, FileStatic> fileSystemView = visitor.getFiles();
        assertEquals(3, fileSystemView.keySet().size());
        assertTrue(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/test1.txt"));
        assertFalse(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/.hidden_file.txt"));
        assertTrue(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/folder-1/test3.txt"));
        assertFalse(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/.hidden/test4.txt"));
        assertFalse(fileSystemView.containsKey(temporaryFolder.getRoot().getAbsolutePath() + "/.hidden"));
    }

    @Test
    public void testVisitorIgnoredFolders() throws IOException {
        temporaryFolder.newFile("test1.txt");
        temporaryFolder.newFile("test2.txt");
        temporaryFolder.newFile(".hidden_file.txt");
        temporaryFolder.newFolder("folder-1");
        temporaryFolder.newFolder(".hidden");
        temporaryFolder.newFile("/folder-1/test3.txt");
        temporaryFolder.newFile("/.hidden/test4.txt");
        temporaryFolder.newFolder("kiwi");
        temporaryFolder.newFile("/kiwi/test5.txt");
        temporaryFolder.newFolder("ega_metadata");
        temporaryFolder.newFile("/ega_metadata/test6.txt");

        RecursiveFileVisitor visitor = new RecursiveFileVisitor(new CompositeAbstractFileListFilter(
                new IgnoreHiddenFileListFilter(), new DirectoryPatternFileListFilter("kiwi|ega_metadata")));

        Files.walkFileTree(temporaryFolder.getRoot().toPath(), new HashSet<>(), Integer.MAX_VALUE, visitor);
        final Map<String, FileStatic> fileSystemView = visitor.getFiles();
        assertEquals(3, fileSystemView.keySet().size());
        String absolutePath = temporaryFolder.getRoot().getAbsolutePath();
        assertTrue(fileSystemView.containsKey(absolutePath + "/test1.txt"));
        assertFalse(fileSystemView.containsKey(absolutePath + "/.hidden_file.txt"));
        assertTrue(fileSystemView.containsKey(absolutePath + "/folder-1/test3.txt"));
        assertFalse(fileSystemView.containsKey(absolutePath + "/.hidden/test4.txt"));
        assertFalse(fileSystemView.containsKey(absolutePath + "/.hidden"));
        assertFalse(fileSystemView.containsKey(absolutePath + "/kiwi/test5.txt"));
        assertFalse(fileSystemView.containsKey(absolutePath + "/ega_metadata/test6.txt"));
    }

}
