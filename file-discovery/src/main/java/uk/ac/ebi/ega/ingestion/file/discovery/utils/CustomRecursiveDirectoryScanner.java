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
package uk.ac.ebi.ega.ingestion.file.discovery.utils;

import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomRecursiveDirectoryScanner extends DefaultDirectoryScanner {

    private int maxDepth = Integer.MAX_VALUE;

    private Set<FileVisitOption> fileVisitOptions = new HashSet<>();

    private boolean ignoreHiddenDirectories = true;

    /**
     * Ignore hidden directories.
     *
     * @param ignoreHiddenDirectories
     */
    public void setIgnoreHiddenDirectories(boolean ignoreHiddenDirectories) {
        this.ignoreHiddenDirectories = ignoreHiddenDirectories;
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

    @Override
    public List<File> listFiles(File directory) throws IllegalArgumentException {
        FileListFilter<File> filter = getFilter();
        boolean supportAcceptFilter = filter instanceof AbstractFileListFilter;

        try {
            CustomFileVisitor visitor = new CustomFileVisitor(ignoreHiddenDirectories);
            Files.walkFileTree(directory.toPath(), this.fileVisitOptions, this.maxDepth, visitor);
            List<File> files = visitor.getFiles();
            Stream<File> fileStream = files.stream()
                    .filter(file -> !supportAcceptFilter
                            || ((AbstractFileListFilter<File>) filter).accept(file));

            final File[] files1 = fileStream.toArray(File[]::new);
            if (supportAcceptFilter) {
                return Arrays.asList(files1);
                //return fileStream.collect(Collectors.toList());
            } else {
                //return filter.filterFiles(fileStream.toArray(File[]::new));
                return filter.filterFiles(files1);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private class CustomFileVisitor implements FileVisitor<Path> {

        private final boolean ignoreHidden;

        private List<File> files;

        public CustomFileVisitor(boolean ignoreHidden) {
            this.ignoreHidden = ignoreHidden;
            this.files = new ArrayList<>();
        }

        public List<File> getFiles() {
            return files;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            Objects.requireNonNull(path);
            Objects.requireNonNull(basicFileAttributes);
            File file = path.toFile();
            if (ignoreHidden && file.isDirectory() && file.isHidden()) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            Objects.requireNonNull(path);
            Objects.requireNonNull(basicFileAttributes);
            this.files.add(path.toFile());
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
}
