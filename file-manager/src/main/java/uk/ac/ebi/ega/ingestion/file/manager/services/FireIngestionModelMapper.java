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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.fire.ingestion.model.FireIngestionModel;
import uk.ac.ebi.ega.fire.ingestion.service.IFireIngestionModelMapper;
import uk.ac.ebi.ega.ingestion.file.manager.kafka.message.EncryptComplete;

public class FireIngestionModelMapper implements IFireIngestionModelMapper<EncryptComplete> {

    @Override
    public FireIngestionModel map(final EncryptComplete source) {
        final FireIngestionModel fireIngestionModel = new FireIngestionModel();

        fireIngestionModel.setEncryptedMd5(source.getEncryptedMd5());
        fireIngestionModel.setEncryptedSize(source.getEncryptedSize());
        fireIngestionModel.setFileType(FileUtils.getType(source.getFileName()));
        fireIngestionModel.setEncryptionPassword(source.getEncryptionPassword());
        fireIngestionModel.setFileName(source.getFileName());
        fireIngestionModel.setStartTime(source.getStartTime());
        fireIngestionModel.setEndTime(source.getEndTime());
        fireIngestionModel.setPlainMd5(source.getPlainMd5());
        fireIngestionModel.setPlainSize(source.getPlainSize());
        fireIngestionModel.setMessage(source.getMessage());
        fireIngestionModel.setStatus(source.getStatus());

        return fireIngestionModel;
    }
}
