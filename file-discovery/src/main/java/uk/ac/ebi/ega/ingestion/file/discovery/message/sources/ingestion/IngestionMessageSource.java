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
package uk.ac.ebi.ega.ingestion.file.discovery.message.sources.ingestion;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.ingestion.commons.models.StagingFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class IngestionMessageSource extends AbstractMessageSource<IngestionEvent> {

    private static final int DEFAULT_INTERNAL_QUEUE_CAPACITY = 5;

    private final BiFunction<String, LocalDateTime, Iterable<? extends StagingFile>> supplier;

    private final Queue<IngestionEvent> toBeReceived;

    private final ReentrantLock lock = new ReentrantLock();

    private volatile long olderThan = 10000;

    private volatile String locationId;

    private volatile String accountId;

    private volatile Path directory;

    public IngestionMessageSource(BiFunction<String, LocalDateTime, Iterable<? extends StagingFile>> supplier) {
        this.toBeReceived = new PriorityBlockingQueue<>(DEFAULT_INTERNAL_QUEUE_CAPACITY, null);
        this.supplier = supplier;
    }

    public void setOlderThan(long olderThan) {
        this.olderThan = olderThan;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    @Override
    protected AbstractIntegrationMessageBuilder<IngestionEvent> doReceive() {
        // rescan only if needed
        if (this.toBeReceived.isEmpty()) {
            if (lock.tryLock()) {
                this.toBeReceived.addAll(scanForEvents());
            }
        }

        // event == null means the queue was empty
        IngestionEvent event = this.toBeReceived.poll();
        if (event != null) {
            return getMessageBuilderFactory().withPayload(event);
        }
        return null;
    }

    private Collection<? extends IngestionEvent> scanForEvents() {
        LocalDateTime cutOff = LocalDateTime.now().minus(olderThan, ChronoUnit.MILLIS);
        ArrayList<IngestionEvent> events = new ArrayList<>();

        HashMap<String, StagingFile> encryptedStagingFiles = new HashMap<>();
        HashMap<String, StagingFile> encryptedStagingFileMd5s = new HashMap<>();
        HashMap<String, StagingFile> plainStagingFileMd5s = new HashMap<>();

        supplier.apply(locationId, cutOff).forEach(stagingFile -> {
            String pathToSearch = stagingFile.getRelativePath();
            if (pathToSearch.endsWith(".gpg")) {
                encryptedStagingFiles.put(pathToSearch.substring(0, pathToSearch.length() - 4), stagingFile);
                return;
            }
            if (pathToSearch.endsWith(".gpg.md5")) {
                encryptedStagingFileMd5s.put(pathToSearch.substring(0, pathToSearch.length() - 8), stagingFile);
                return;
            }
            if (pathToSearch.endsWith(".md5")) {
                plainStagingFileMd5s.put(pathToSearch.substring(0, pathToSearch.length() - 4), stagingFile);
                return;
            }
        });
        encryptedStagingFiles.keySet().forEach(s -> {
            if (encryptedStagingFileMd5s.containsKey(s) && plainStagingFileMd5s.containsKey(s)) {
                StagingFile encryptedStagingFile = encryptedStagingFiles.get(s);
                StagingFile encryptedStagingFileMd5 = encryptedStagingFileMd5s.get(s);
                StagingFile plainStagingFileMd5 = plainStagingFileMd5s.get(s);
                events.add(new IngestionEvent(accountId, locationId, directory, encryptedStagingFile.toFileStatic(),
                        plainStagingFileMd5.toFileStatic(), encryptedStagingFileMd5.toFileStatic()));
            }
        });

        return events;
    }

    @Override
    public String getComponentType() {
        return "ingestion-event:inbound-channel-adapter";
    }

}
