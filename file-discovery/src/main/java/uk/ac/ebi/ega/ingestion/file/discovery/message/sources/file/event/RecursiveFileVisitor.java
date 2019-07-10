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

import org.springframework.integration.file.filters.AbstractFileListFilter;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecursiveFileVisitor implements FileVisitor<Path> {

    private final HashMap<String, FileStatic> files;

    private final AbstractFileListFilter<File> filter;

    public RecursiveFileVisitor() {
        this(new AbstractFileListFilter<File>() {
            @Override
            public boolean accept(File file) {
                return true;
            }
        });
    }

    public RecursiveFileVisitor(AbstractFileListFilter<File> filter) {
        this.files = new HashMap<>();
        this.filter = filter;
    }

    public Map<String, FileStatic> getFiles() {
        return files;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(basicFileAttributes);
        File file = path.toFile();
        if (file.isDirectory() && !filter.accept(file)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(basicFileAttributes);
        File file = path.toFile();
        if (filter.accept(file)) {
            FileStatic fileStatic = new FileStatic(file);
            this.files.put(fileStatic.getAbsolutePath(), fileStatic);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
        Objects.requireNonNull(path);
        throw e;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        Objects.requireNonNull(path);
        if (e != null) {
            throw e;
        } else {
            return FileVisitResult.CONTINUE;
        }
    }

}
