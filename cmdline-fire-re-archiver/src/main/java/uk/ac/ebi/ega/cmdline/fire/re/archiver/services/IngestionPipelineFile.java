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
package uk.ac.ebi.ega.cmdline.fire.re.archiver.services;

import java.io.File;

public class IngestionPipelineFile {

    private File file;
    private String md5;
    private long fileSize;

    public IngestionPipelineFile(File file, String md5, long fileSize) {
        this.file = file;
        this.md5 = md5;
        this.fileSize = fileSize;
    }

    public File getFile() {
        return file;
    }

    public String getMd5() {
        return md5;
    }

    public long getFileSize() {
        return fileSize;
    }
}
