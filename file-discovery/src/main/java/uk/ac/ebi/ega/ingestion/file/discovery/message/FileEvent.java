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
package uk.ac.ebi.ega.ingestion.file.discovery.message;

import java.io.File;

public class FileEvent implements Comparable<FileEvent> {

    public enum Type {
        CREATED(1),

        UPDATED(2),

        DELETED(3),

        INGEST(0);

        private int priority;

        Type(int priority) {
            this.priority = priority;
        }
    }

    private Type type;

    private String absolutePath;

    private long lastModified;

    public FileEvent(Type type, String absolutePath, long lastModified) {
        this.type = type;
        this.absolutePath = absolutePath;
        this.lastModified = lastModified;
    }

    public static FileEvent ingest(String absolutePath, long lastModified) {
        return new FileEvent(Type.INGEST, absolutePath, lastModified);
    }

    public static FileEvent updated(String absolutePath, long lastModified) {
        return new FileEvent(Type.UPDATED, absolutePath, lastModified);
    }

    public static FileEvent created(String absolutePath, long lastModified) {
        return new FileEvent(Type.CREATED, absolutePath, lastModified);
    }

    public static FileEvent deleted(String absolutePath, long lastModified) {
        return new FileEvent(Type.DELETED, absolutePath, lastModified);
    }

    @Override
    public int compareTo(FileEvent fileEvent) {
        int value = type.compareTo(fileEvent.type);
        if (value != 0) {
            return value;
        }
        return absolutePath.compareTo(fileEvent.absolutePath);
    }

    @Override
    public String toString() {
        return "FileEvent{" +
                "type=" + type +
                ", absolutePath='" + absolutePath + '\'' +
                ", lastModified=" + lastModified +
                '}';
    }
}
