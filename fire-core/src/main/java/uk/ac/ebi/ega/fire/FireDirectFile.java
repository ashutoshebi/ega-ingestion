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
package uk.ac.ebi.ega.fire;

import java.io.IOException;
import java.io.InputStream;

public class FireDirectFile implements IFireFile {

    private final String urlGet;

    private final String urlHead;

    private final String md5;

    private final long size;

    private final String expirationDate;

    private final String storageClass;

    public FireDirectFile(String urlGet, String urlHead, String md5, long size, String expirationDate,
                          String storageClass) {
        this.urlGet = urlGet;
        this.urlHead = urlHead;
        this.md5 = md5;
        this.size = size;
        this.expirationDate = expirationDate;
        this.storageClass = storageClass;
    }

    @Override
    public String getMd5() {
        return md5;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new FireDirectInputStream(urlGet, urlHead);
    }

    public boolean isStorageS3() {
        return storageClass.equals("S3");
    }

    public String getStorageClass() {
        return storageClass;
    }
}
