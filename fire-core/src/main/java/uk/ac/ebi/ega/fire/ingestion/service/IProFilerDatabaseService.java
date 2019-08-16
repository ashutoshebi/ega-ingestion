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

public interface IProFilerDatabaseService {

    long archiveFile(String egaFileId, File file, String md5, String pathOnFire);

    /**
     * Returns those rows (as OldFireFile objects)
     * from the ega-pro-filer.ega_ARCHIVE.archive table
     * where the archive_id is equal to the given fireIds.
     *
     * @param fireIds ega-pro-filer.ega_ARCHIVE.archive.archive_id's
     * @return the DB-rows as a list of OldFireFile object
     */
    List<OldFireFile> findAllByFireId(final List<Long> fireIds);
}
