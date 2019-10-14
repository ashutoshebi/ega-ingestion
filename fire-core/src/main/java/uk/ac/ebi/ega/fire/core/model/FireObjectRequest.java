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
package uk.ac.ebi.ega.fire.core.model;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Model class to map FIRE request parameters.
 */
public class FireObjectRequest {
    private final File fileToUpload;
    private final Map<String, String> headers;

    public FireObjectRequest(final File fileToUpload, final Map<String, String> headers) {
        this.fileToUpload = Objects.requireNonNull(fileToUpload, "File to upload can't be null");
        this.headers = Objects.requireNonNull(headers, "Header map can't be null. Pass empty map if no value present.");
    }

    public File getFileToUpload() {
        return fileToUpload;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public String toString() {
        return "FireObjectRequest{" +
                "fileToUpload=" + fileToUpload +
                ", headers=" + headers +
                '}';
    }
}
