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
package uk.ac.ebi.ega.ingestion.file.manager.reports;

import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;

import java.util.Arrays;

public class FileDetailsTsv extends TsvReport<IFileDetails> {

    public FileDetailsTsv(String fileName) {
        super(
                fileName,
                Arrays.asList(
                        "ACCOUNT_ID",
                        "STAGING_ID",
                        "PATH",
                        "VERSION",
                        "PLAIN_MD5",
                        "PLAIN_SIZE",
                        "ARCHIVE_STATUS"),
                iFileDetails -> new String[]{
                        iFileDetails.getAccountId(),
                        iFileDetails.getStagingId(),
                        iFileDetails.getPath(),
                        Long.toString(iFileDetails.getVersion()),
                        iFileDetails.getPlainMd5(),
                        iFileDetails.getPlainSize() == null ? "N/A" : iFileDetails.getPlainSize().toString(),
                        iFileDetails.getStatus().toString()});
    }

}
