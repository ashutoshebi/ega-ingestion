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
package uk.ac.ebi.ega.ingestion.commons.messages;

import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileEvent implements Comparable<FileEvent> {

    public enum Type {

        CREATED(1),

        UPDATED(2),

        DELETED(3);

        private int priority;

        Type(int priority) {
            this.priority = priority;
        }

    }

    private Type type;

    private String locationId;

    private String locationPath;

    private String relativePath;

    private long size;

    private long lastModified;

    public FileEvent(Type type, String locationId, Path directory, FileStatic file) {
        this(type, locationId, directory.toString(), directory.relativize(Paths.get(file.getAbsolutePath())).toString(),
                file.length(), file.lastModified());
    }

    public FileEvent(Type type, String locationId, String locationPath, String relativePath, long size,
                     long lastModified) {
        this.type = type;
        this.locationId = locationId;
        this.locationPath = locationPath;
        this.relativePath = relativePath;
        this.size = size;
        this.lastModified = lastModified;
    }

    public Type getType() {
        return type;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getLocationPath() {
        return locationPath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public static FileEvent updated(String locationId, Path directory, FileStatic file) {
        return new FileEvent(Type.UPDATED, locationId, directory, file);
    }

    public static FileEvent created(String locationId, Path directory, FileStatic file) {
        return new FileEvent(Type.CREATED, locationId, directory, file);
    }

    public static FileEvent deleted(String locationId, Path directory, FileStatic file) {
        return new FileEvent(Type.DELETED, locationId, directory, file);
    }

    @Override
    public int compareTo(FileEvent fileEvent) {
        int value = type.compareTo(fileEvent.type);
        if (value != 0) {
            return value;
        }
        return relativePath.compareTo(fileEvent.relativePath);
    }

    @Override
    public String toString() {
        return "FileEvent{" +
                "type=" + type +
                ", locationId='" + locationId + '\'' +
                ", locationPath='" + locationPath + '\'' +
                ", relative='" + relativePath + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                '}';
    }
}
