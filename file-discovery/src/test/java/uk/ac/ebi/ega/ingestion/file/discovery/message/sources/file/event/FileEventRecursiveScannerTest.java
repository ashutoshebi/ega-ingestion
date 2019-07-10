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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class FileEventRecursiveScannerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void filesCreatedBeforeFirstScan() throws IOException {
        temporaryFolder.newFile("test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(1, fileEvents.size());
        Assert.assertEquals(FileEvent.Type.CREATED, fileEvents.get(0).getType());
    }

    @Test
    public void filesCreatedBeforeFirstScanButAlreadyKnown() throws IOException {
        final File test1Txt = temporaryFolder.newFile("test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        scanner.initializeDirectoryStatus(createState(test1Txt));
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(0, fileEvents.size());
    }

    private HashMap<String, FileStatic> createState(File... files) {
        HashMap<String, FileStatic> state = new HashMap<>();
        for (File file : files) {
            state.put(file.getAbsolutePath(), new FileStatic(file));
        }
        return state;
    }

    @Test
    public void filesCreatedThenDeleted() throws IOException {
        final File test1Txt = temporaryFolder.newFile("test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        final List<FileEvent> createEvent = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());
        Assert.assertEquals(1, createEvent.size());

        test1Txt.delete();
        final List<FileEvent> deleteEvent = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());
        Assert.assertEquals(1, deleteEvent.size());
        Assert.assertEquals(FileEvent.Type.DELETED, deleteEvent.get(0).getType());
    }

    @Test
    public void filesCreatedThenModified() throws IOException, InterruptedException {
        final File test1Txt = temporaryFolder.newFile("test1.txt");
        long var1 = test1Txt.lastModified();

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        final List<FileEvent> createEvent = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());
        Assert.assertEquals(1, createEvent.size());

        //We need to wait so that it is not modified in the same ms.
        Thread.sleep(1000);
        FileOutputStream stream = new FileOutputStream(test1Txt);
        stream.write("kiwi".getBytes());
        stream.close();
        long var2 = test1Txt.lastModified();

        final List<FileEvent> deleteEvent = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());
        Assert.assertEquals(1, deleteEvent.size());
        Assert.assertEquals(FileEvent.Type.UPDATED, deleteEvent.get(0).getType());
    }

    @Test
    public void ignoreHiddenFiles() throws IOException {
        temporaryFolder.newFile(".test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(0, fileEvents.size());
    }

    @Test
    public void fileCreateInSubDirectory() throws IOException {
        temporaryFolder.newFolder("test");
        temporaryFolder.newFile("test/test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(1, fileEvents.size());
        Assert.assertEquals(FileEvent.Type.CREATED, fileEvents.get(0).getType());
    }

    @Test
    public void ignoreFilesInHiddenDirectories() throws IOException {
        temporaryFolder.newFolder("notHidden");
        temporaryFolder.newFile("notHidden/kiwi");
        temporaryFolder.newFolder(".hidden");
        temporaryFolder.newFile(".hidden/test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(1, fileEvents.size());
    }

    @Test
    public void ignoreRegexDoesNotAffectFiles() throws IOException {
        temporaryFolder.newFolder("notHidden");
        temporaryFolder.newFile("notHidden/kiwi");
        temporaryFolder.newFolder(".hidden");
        temporaryFolder.newFile(".hidden/test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        scanner.setFilter(new CompositeAbstractFileListFilter(
                new IgnoreHiddenFileListFilter(), new DirectoryPatternFileListFilter("kiwi")));
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(1, fileEvents.size());
    }

    @Test
    public void ignoreRegex() throws IOException {
        temporaryFolder.newFolder("notHidden");
        temporaryFolder.newFile("notHidden/kiwi");
        temporaryFolder.newFolder(".hidden");
        temporaryFolder.newFile(".hidden/test1.txt");

        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        scanner.setFilter(new CompositeAbstractFileListFilter(
                new IgnoreHiddenFileListFilter(), new DirectoryPatternFileListFilter("notHidden")));
        final List<FileEvent> fileEvents = scanner.listFileEvents("location-test", temporaryFolder.getRoot().toPath());

        Assert.assertEquals(0, fileEvents.size());
    }


}
