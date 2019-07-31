package uk.ac.ebi.ega.cmdline.fire.re.archiver.services;

import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineResult;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;

public interface IReEncryptionService {

    IngestionPipelineResult reEncrypt(final File origin, final File output) throws SystemErrorException, UserErrorException;

}
