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
package uk.ac.ebi.ega.ingestion.file.discovery.models;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;

public class FileSystemNode {

    private FileSystemNode parent;

    private HashMap<String, FileSystemNode> children;

    private String name;

    private boolean isDirectory;

    private Long size;

    private LocalDateTime createDate;

    private LocalDateTime updateDate;

    private FileSystemNode(String name, boolean isDirectory) {
        this(null, new HashMap<>(), name, isDirectory, null, null, null);
    }

    private FileSystemNode(FileSystemNode parent, String name, Long size, LocalDateTime createDate,
                           LocalDateTime updateDate) {
        this(parent, new HashMap<>(), name, false, size, createDate, updateDate);
    }

    private FileSystemNode(FileSystemNode parent, HashMap<String, FileSystemNode> children, String name,
                           boolean isDirectory, Long size, LocalDateTime createDate, LocalDateTime updateDate) {
        this.parent = parent;
        this.children = children;
        this.name = name;
        this.isDirectory = isDirectory;
        this.size = size;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }


    public static FileSystemNode root(String name) {
        return directory(name);
    }

    private static FileSystemNode directory(String name) {
        return new FileSystemNode(name, true);
    }

    private static FileSystemNode file(FileSystemNode parent, String name, Long size, LocalDateTime createDate,
                                       LocalDateTime updateDate) {
        return new FileSystemNode(parent, name, size, createDate, updateDate);
    }

    public void addChild(StagingAreaFile stagingAreaFile) {
        Path relativePath = Paths.get(stagingAreaFile.getRelativePath());
        FileSystemNode directory = getOrCreatePath(relativePath);
        directory.addChild(file(directory, stagingAreaFile.getName(), stagingAreaFile.getSize(),
                stagingAreaFile.getCreateDate(), stagingAreaFile.getUpdateDate()));
    }

    private FileSystemNode getOrCreatePath(Path path) {
        FileSystemNode node = this;
        int nameCount = path.getNameCount();
        for (int i = 0; i < nameCount; i++) {
            node = node.getOrCreateDirectory(path.getName(i).toString());
        }
        return node;
    }

    public FileSystemNode getOrCreateDirectory(String name) {
        if (children.containsKey(name)) {
            return children.get(name);
        } else {
            return addChild(directory(name));
        }
    }

    private FileSystemNode addChild(FileSystemNode file) {
        children.put(file.name, file);
        return file;
    }

    public void addChildren(Iterable<? extends StagingAreaFile> fileIterable) {
        fileIterable.forEach(stagingAreaFile -> {
            addChild(stagingAreaFile);
        });
    }
}
