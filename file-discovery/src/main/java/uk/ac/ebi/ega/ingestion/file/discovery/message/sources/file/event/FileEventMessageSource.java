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

import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.util.Assert;
import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;

import java.io.File;
import java.util.Queue;
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

    private final ReentrantLock lock = new ReentrantLock();

    private final FileEventRecursiveDirectoryScanner scanner;

    private volatile String locationId;

    private volatile File directory;

    private volatile boolean autoCreateDirectory = true;

    public FileEventMessageSource() {
        this(new FileEventRecursiveDirectoryScanner());
    }

    public FileEventMessageSource(FileEventRecursiveDirectoryScanner scanner) {
        this.toBeReceived = new PriorityBlockingQueue<>(DEFAULT_INTERNAL_QUEUE_CAPACITY, null);
        this.scanner = scanner;
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

    /**
     * Specify the location id for the events
     *
     * @param locationId
     */
    public void setLocationId(String locationId) {
        this.locationId = locationId;
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

    @Override
    public void stop() {
        running.getAndSet(false);
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
                this.toBeReceived.addAll(this.scanner.listFileEvents(locationId, this.directory.toPath()));
            }
        }

        // file == null means the queue was empty
        FileEvent fileEvent = this.toBeReceived.poll();
        if (fileEvent != null) {
            return getMessageBuilderFactory().withPayload(fileEvent);
        }
        return null;
    }

    @Override
    public String getComponentType() {
        return "file-event:inbound-channel-adapter";
    }

    @Override
    protected void onInit() {
        Assert.notNull(this.directory, "'directory' must not be null");
    }

}
