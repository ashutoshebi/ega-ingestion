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
package uk.ac.ebi.ega.ingestion.file.manager.models;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.TimeZone;

public class FileSystemNode {

    private FileSystemNode parent;

    private HashMap<String, FileSystemNode> children;

    private String path;

    private boolean isDirectory;

    private Long size;

    private LocalDateTime updateDate;

    private FileSystemNode(String path, boolean isDirectory) {
        this(null, new HashMap<>(), path, isDirectory, null, null);
    }

    private FileSystemNode(FileSystemNode parent, String path, Long size, LocalDateTime updateDate) {
        this(parent, new HashMap<>(), path, false, size, updateDate);
    }

    private FileSystemNode(FileSystemNode parent, HashMap<String, FileSystemNode> children, String path,
                           boolean isDirectory, Long size, LocalDateTime updateDate) {
        this.parent = parent;
        this.children = children;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = size;
        this.updateDate = updateDate;
    }


    public static FileSystemNode root(String path) {
        return directory(path);
    }

    private static FileSystemNode directory(String name) {
        return new FileSystemNode(name, true);
    }

    private static FileSystemNode file(FileSystemNode parent, String name, Long size, LocalDateTime updateDate) {
        return new FileSystemNode(parent, name, size, updateDate);
    }

    public void addChild(StagingFile stagingFile) {
        final Path path = Paths.get(stagingFile.getRelativePath());
        addChild(path.getParent(), path.getFileName().toString(), stagingFile.getFileSize(),
                stagingFile.getUpdateDate());
    }

    public void addChild(File file) {
        Path relativePath = Paths.get(path).relativize(Paths.get(file.getParentFile().getAbsolutePath()));
        addChild(relativePath, file.getName(), file.length(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), TimeZone.getDefault().toZoneId()));
    }

    private void addChild(Path relativePath, String name, long size, LocalDateTime updateDate) {
        FileSystemNode directory = getOrCreatePath(relativePath);
        directory.addChild(file(directory, name, size, updateDate));
    }


    private FileSystemNode getOrCreatePath(Path path) {
        FileSystemNode node = this;
        int nameCount = path.getNameCount();
        for (int i = 0; i < nameCount; i++) {
            node = node.getOrCreateDirectory(path.getName(i).toString());
        }
        return node;
    }

    private FileSystemNode getOrCreateDirectory(String name) {
        if (name.isEmpty()) {
            return this;
        }
        if (children.containsKey(name)) {
            return children.get(name);
        } else {
            return addChild(directory(name));
        }
    }

    private FileSystemNode addChild(FileSystemNode file) {
        children.put(file.path, file);
        return file;
    }

    public void addChildren(Iterable<? extends StagingFile> fileIterable) {
        fileIterable.forEach(this::addChild);
    }

    public long getChildrenCount() {
        return children.values().size();
    }

    /**
     * Returns the children with the name or route. Returns null if file or path could not be found.
     *
     * @param name
     * @return
     */
    public FileSystemNode getChild(String name) {
        FileSystemNode node = this;
        Path path = Paths.get(name);
        int nameCount = path.getNameCount();
        for (int i = 0; i < nameCount; i++) {
            node = node.children.get(path.getName(i).toString());
            if (node == null) {
                break;
            }
        }
        return node;
    }
}
