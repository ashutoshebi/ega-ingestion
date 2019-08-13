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
package uk.ac.ebi.ega.fire.ingestion.service;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface IFireService {

    Optional<Long> archiveFile(String egaFileId, File file, String md5, String pathOnFire);

    /**
     * Joins the ega-pro-filer.ega_ARCHIVE.archive and ega-pro-filer.ega_ARCHIVE.file tables
     * then returns those rows (as OldFireFile objects)
     * where the ega-pro-filer.ega_ARCHIVE.file.file_id is equal to the given fileIds.
     * @param fileIds ega-pro-filer.ega_ARCHIVE.file.file_id's
     * @return the DB-rows as a list of OldFireFile object
     */
    List<OldFireFile> findAllByFileId(List<Long> fileIds);

}
