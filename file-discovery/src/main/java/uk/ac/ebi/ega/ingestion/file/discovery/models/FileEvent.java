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

import java.io.File;

public class FileEvent {

    public enum Type {
        CREATED,

        UPDATED,

        DELETED,

        INGEST
    }

    private Type type;

    private String absolutePath;

    public FileEvent(Type type, String absolutePath) {
        this.type = type;
        this.absolutePath = absolutePath;
    }

    public static FileEvent ingest(String absolutePath) {
        return new FileEvent(Type.INGEST, absolutePath);
    }

    public static FileEvent updated(File directory, String absolutePath) {
        return new FileEvent(Type.UPDATED, absolutePath);
    }

    public static FileEvent created(File directory, String absolutePath) {
        return new FileEvent(Type.CREATED, absolutePath);
    }

    public static FileEvent deleted(File directory, String absolutePath) {
        return new FileEvent(Type.DELETED, absolutePath);
    }
}
