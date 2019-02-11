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
package uk.ac.ebi.ega.ingestion.file.discovery.persistence;

import uk.ac.ebi.ega.ingestion.file.discovery.models.FileSystemNode;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingAreaFileRepository;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingAreaRepository;

public class StagingAreaServiceImpl implements StagingAreaService {

    private StagingAreaRepository repository;

    private StagingAreaFileRepository fileRepository;

    public StagingAreaServiceImpl(StagingAreaRepository repository, StagingAreaFileRepository fileRepository) {
        this.repository = repository;
        this.fileRepository = fileRepository;
    }

    @Override
    public Iterable<? extends StagingArea> findAll() {
        return repository.findAll();
    }

    @Override
    public Iterable<? extends StagingArea> findAllEnabled() {
        return repository.findAllByEnabled(true);
    }

    @Override
    public FileSystemNode getFilesOfStagingArea(String stagingAreaId) {
        final FileSystemNode root = FileSystemNode.root(stagingAreaId);
        root.addChildren(fileRepository.findAllByStagingAreaId(stagingAreaId));
        return root;
    }

}
