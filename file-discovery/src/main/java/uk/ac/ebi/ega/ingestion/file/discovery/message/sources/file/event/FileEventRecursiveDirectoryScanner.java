/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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

import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.util.Assert;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileEventRecursiveDirectoryScanner {

    private int maxDepth = Integer.MAX_VALUE;

    private Set<FileVisitOption> fileVisitOptions = new HashSet<>();

    private volatile AbstractFileListFilter<File> filter;

    private volatile Map<String, FileStatic> fileSystemView;

    public FileEventRecursiveDirectoryScanner() {
        this.filter = new IgnoreHiddenFileListFilter();
        this.fileSystemView = new HashMap<>();
    }

    /**
     * Sets a custom filter to be used by this scanner. The filter will get a
     * chance to reject fileSystemView before the scanner presents the event sequence.
     *
     * @param filter the custom filter to be used
     */
    public void setFilter(AbstractFileListFilter<File> filter) {
        this.filter = filter;
    }

    /**
     * The maximum number of directory levels to visit.
     *
     * @param maxDepth the maximum number of directory levels to visit
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * The options to configure the traversal.
     *
     * @param fileVisitOptions options to configure the traversal
     */
    public void setFileVisitOptions(FileVisitOption... fileVisitOptions) {
        Assert.notNull(fileVisitOptions, "'fileVisitOptions' must not be null");
        this.fileVisitOptions.clear();
        this.fileVisitOptions.addAll(Arrays.asList(fileVisitOptions));
    }

    public List<FileEvent> listFileEvents(String locationId, Path directory) throws IllegalArgumentException {
        try {
            RecursiveFileVisitor visitor = new RecursiveFileVisitor(filter);
            Files.walkFileTree(directory, this.fileVisitOptions, this.maxDepth, visitor);
            return update(locationId, directory, visitor.getFiles());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<FileEvent> update(String locationId, Path directory, Map<String, FileStatic> newFileSystemView) {
        List<FileEvent> events = new ArrayList<>();
        newFileSystemView.forEach((absolutePath, newFile) -> {
            FileStatic currentFile = fileSystemView.get(absolutePath);
            if (currentFile == null) {
                events.add(FileEvent.created(locationId, directory, newFile));
            } else {
                fileSystemView.remove(absolutePath);
                if (currentFile.lastModified() < newFile.lastModified()) {
                    events.add(FileEvent.updated(locationId, directory, newFile));
                }
            }
        });
        fileSystemView.values().stream().forEach(file -> events.add(FileEvent.deleted(locationId, directory, file)));
        fileSystemView = newFileSystemView;
        return events;
    }

    public void initializeDirectoryStatus(Map<String, FileStatic> fileSystemView) {
        this.fileSystemView = fileSystemView;
    }
}
