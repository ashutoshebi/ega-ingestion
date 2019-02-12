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
package uk.ac.ebi.ega.ingestion.file.discovery.message.sources;

import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.util.Assert;
import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.StagingAreaService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class FileEventMessageSource extends AbstractMessageSource<FileEvent>
        implements Lifecycle {

    private static final int DEFAULT_INTERNAL_QUEUE_CAPACITY = 5;

    private final AtomicBoolean running = new AtomicBoolean();

    /*
     * {@link PriorityBlockingQueue#iterator()} throws
     * {@link java.util.ConcurrentModificationException} in Java 5.
     * There is no locking around the queue, so there is also no iteration.
     */
    private final Queue<FileEvent> toBeReceived;

    private final HashMap<String, Long> currentFiles = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private volatile StagingAreaService stagingAreaService;

    private volatile File directory;

    private volatile DirectoryScanner scanner = new DefaultDirectoryScanner();

    private volatile boolean scannerExplicitlySet;

    private volatile boolean autoCreateDirectory = true;

    private volatile long olderThan = 10000;

    private FileListFilter<File> filter;

    public FileEventMessageSource() {
        this.toBeReceived = new PriorityBlockingQueue<>(DEFAULT_INTERNAL_QUEUE_CAPACITY, null);
    }

    @Override
    public void start() {
        if (!this.running.getAndSet(true)) {
            if (!this.directory.exists() && this.autoCreateDirectory) {
                this.directory.mkdirs();
            }
            Assert.isTrue(this.directory.exists(),
                    "Source directory [" + this.directory + "] does not exist.");
            Assert.isTrue(this.directory.isDirectory(),
                    "Source path [" + this.directory + "] does not point to a directory.");
            Assert.isTrue(this.directory.canRead(),
                    "Source directory [" + this.directory + "] is not readable.");
            if (this.scanner instanceof Lifecycle) {
                ((Lifecycle) this.scanner).start();
            }
        }
    }

    public void setStagingAreaService(StagingAreaService stagingAreaService) {
        this.stagingAreaService = stagingAreaService;
    }

    /**
     * Specify the cutoff for files
     *
     * @param olderThan
     */
    public void setOlderThan(long olderThan) {
        this.olderThan = olderThan;
    }

    /**
     * Specify the input directory.
     *
     * @param directory to monitor
     */
    public void setDirectory(File directory) {
        Assert.notNull(directory, "directory must not be null");
        this.directory = directory;
    }

    /**
     * Optionally specify a custom scanner
     *
     * @param scanner scanner implementation
     */
    public void setScanner(DirectoryScanner scanner) {
        Assert.notNull(scanner, "'scanner' must not be null.");
        this.scanner = scanner;
        this.scannerExplicitlySet = true;
    }

    /**
     * The {@link #scanner} property accessor to allow to modify its options
     * ({@code filter}, {@code locker} etc.) at runtime using the
     * {@link FileReadingMessageSource} bean.
     *
     * @return the {@link DirectoryScanner} of this {@link FileReadingMessageSource}.
     * @since 4.2
     */
    public DirectoryScanner getScanner() {
        return this.scanner;
    }

    /**
     * Specify whether to create the source directory automatically if it does
     * not yet exist upon initialization. By default, this value is
     * <em>true</em>. If set to <em>false</em> and the
     * source directory does not exist, an Exception will be thrown upon
     * initialization.
     *
     * @param autoCreateDirectory should the directory to be monitored be created when this
     *                            component starts up?
     */
    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    /**
     * Sets a {@link FileListFilter}.
     * By default a {@link org.springframework.integration.file.filters.AcceptOnceFileListFilter}
     * with no bounds is used. In most cases a customized {@link FileListFilter} will
     * be needed to deal with modification and duplication concerns.
     * If multiple filters are required a
     * {@link org.springframework.integration.file.filters.CompositeFileListFilter}
     * can be used to group them together.
     * <p>
     * <b>The supplied filter must be thread safe.</b>.
     *
     * @param filter a filter
     */
    public void setFilter(FileListFilter<File> filter) {
        Assert.notNull(filter, "'filter' must not be null");
        this.filter = filter;
    }

    @Override
    public void stop() {
        if (this.running.getAndSet(false) && this.scanner instanceof Lifecycle) {
            ((Lifecycle) this.scanner).stop();
        }
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    protected AbstractIntegrationMessageBuilder<FileEvent> doReceive() {
        // rescan only if needed
        if (this.toBeReceived.isEmpty()) {
            if (lock.tryLock()) {
                scanInputDirectory();
            }
        }

        // file == null means the queue was empty
        FileEvent fileEvent = this.toBeReceived.poll();
        if (fileEvent != null) {
            return getMessageBuilderFactory()
                    .withPayload(fileEvent);
        }

        return null;
    }

    private void scanInputDirectory() {
        List<File> filteredFiles = this.scanner.listFiles(this.directory);
        updateFiles(filteredFiles);
    }

    private void updateFiles(List<File> newCurrentFiles) {
        List<File> newFiles = new ArrayList<>();
        List<File> updatedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        Set<String> stableFileNames = new HashSet<>();
        long cutOff = System.currentTimeMillis() - olderThan;

        newCurrentFiles.forEach(file -> {
            String absolutePath = file.getAbsolutePath();
            long lastModified = file.lastModified();
            if (currentFiles.containsKey(absolutePath)) {
                if (lastModified > currentFiles.get(absolutePath)) {
                    addUpdatedEvent(absolutePath, lastModified);
                    updatedFiles.add(file);
                } else {
                    if (lastModified < cutOff) {
                        stableFileNames.add(absolutePath);
                        if (checkIfFileAndMd5ArePresent(stableFileNames, absolutePath)) {
                            addFileToIngestEvent(removeDotMd5IfNeeded(absolutePath), lastModified);
                        }
                    }
                }
            } else {
                addCreatedEvent(absolutePath, lastModified);
                newFiles.add(file);
            }
            currentFiles.remove(absolutePath);
        });
        currentFiles.forEach((absolutePath, lastModified) -> {
            addDeletedEvent(absolutePath, lastModified);
            deletedFiles.add(absolutePath);
        });
        currentFiles.clear();
        newCurrentFiles.forEach(file -> currentFiles.put(file.getAbsolutePath(), file.lastModified()));
//        stagingAreaService.update();
    }

    private String removeDotMd5IfNeeded(String absolutePath) {
        if (absolutePath.endsWith(".md5")) {
            return absolutePath.substring(0, absolutePath.length() - 4);
        } else {
            return absolutePath;
        }
    }

    private boolean checkIfFileAndMd5ArePresent(Set<String> stableFileNames, String absolutePath) {
        if (absolutePath.endsWith(".md5")) {
            return stableFileNames.contains(absolutePath.substring(0, absolutePath.length() - 4));
        } else {
            return stableFileNames.contains(absolutePath.concat(".md5"));
        }
    }

    private void addFileToIngestEvent(String absolutePath, long lastModified) {
        toBeReceived.add(FileEvent.ingest(absolutePath, lastModified));
    }

    private void addCreatedEvent(String absolutePath, long lastModified) {
        toBeReceived.add(FileEvent.created(absolutePath, lastModified));
    }

    private void addUpdatedEvent(String absolutePath, long lastModified) {
        toBeReceived.add(FileEvent.updated(absolutePath, lastModified));
    }

    private void addDeletedEvent(String absolutePath, long lastModified) {
        toBeReceived.add(FileEvent.deleted(absolutePath, lastModified));
    }

    @Override
    public String getComponentType() {
        return "file-event:inbound-channel-adapter";
    }

    @Override
    protected void onInit() {
        Assert.notNull(this.directory, "'directory' must not be null");

        // Check that the filter and locker options are _NOT_ set if an external scanner has been set.
        // The external scanner is responsible for the filter and locker options in that case.
        Assert.state(!(this.scannerExplicitlySet && this.filter != null),
                "When using an external scanner the 'filter' option should not be used. Instead, set that " +
                        "options on the external DirectoryScanner: " + this.scanner);
        if (this.filter != null) {
            this.scanner.setFilter(this.filter);
        }
    }
}
