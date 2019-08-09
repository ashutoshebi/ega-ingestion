package uk.ac.ebi.ega.cmdline.fire.re.archiver.services;

import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineResult;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;

public interface IReEncryptionService {

    /**
     * First decrypt the given GPG-encrypted input-file
     * and then encrypt it using Alexander's AES flavour
     * ({@link uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega}).
     */
    IngestionPipelineResult reEncrypt(final File gpgEncryptedInputFile,
                                      final File aesCtr256EgaEncryptedOutputFile)
            throws SystemErrorException, UserErrorException;

}
