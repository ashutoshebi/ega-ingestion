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

import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.StreamPipelineBuilder;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongPassword;
import uk.ac.ebi.ega.encryption.core.stream.PipelineStream;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineFile;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineResult;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReEncryptionService implements IReEncryptionService {

    private final List<File> outputFiles;
    private File origin;
    private File output;
    private char[] password;
    private File secretRing;
    private File secretRingKey;

    public ReEncryptionService(final File secretRing, final File secretRingKey, final char[] password) {
        this.outputFiles = new ArrayList<>();
        this.secretRing = secretRing;
        this.secretRingKey = secretRingKey;
        this.password = password;
    }

    @Override
    public final IngestionPipelineResult reEncrypt(final File origin, final File output) throws SystemErrorException, UserErrorException {
        this.origin = origin;
        this.output = output;

        try {
            return doProcess();
        } catch (SystemErrorException e) {
            deleteOutputFiles();
            throw e;
        } catch (IOException | WrongPassword e) {
            //If it is an io error or the pgp keyring password is wrong
            deleteOutputFiles();
            throw new SystemErrorException(e);
        } catch (AlgorithmInitializationException e) {
            deleteOutputFiles();
            throw new UserErrorException(e);
        }
    }

    private IngestionPipelineResult doProcess() throws SystemErrorException, IOException,
            AlgorithmInitializationException {
        try (
                final DecryptInputStream decryptInputStream = getDecryptionInputStream();
                final EncryptOutputStream encryptOutputStream = getEncryptionOutputStream(output);
                final PipelineStream stream = StreamPipelineBuilder
                        .source(decryptInputStream)
                        .to(encryptOutputStream)
                        .build()
        ) {
            final long bytesTransferred = stream.execute();

            return new IngestionPipelineResult(
                    new IngestionPipelineFile(origin, decryptInputStream.getMd5(), decryptInputStream.available()),
                    decryptInputStream.getUnencryptedMd5(),
                    bytesTransferred,
                    password,
                    new IngestionPipelineFile(output, encryptOutputStream.getMd5(), output.length())
            );
        }
    }

    private DecryptInputStream getDecryptionInputStream() throws AlgorithmInitializationException, IOException {
        return new DecryptInputStream(
                new FileInputStream(origin),
                new PgpKeyring(new FileInputStream(secretRing)),
                FileUtils.readPasswordFile(secretRingKey.toPath()));
    }

    private EncryptOutputStream getEncryptionOutputStream(final File file) throws SystemErrorException {
        createFile(file);
        try {
            return new EncryptOutputStream(new FileOutputStream(file), new AesCtr256Ega(), password);
        } catch (Exception e) {
            throw new SystemErrorException(e);
        }
    }

    private void deleteOutputFiles() throws SystemErrorException {
        for (File file : outputFiles) {
            if (file.exists() && !file.delete()) {
                throw new SystemErrorException("File could not be deleted");
            }
        }
    }

    private void createFile(final File file) throws SystemErrorException {
        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new SystemErrorException("File could not be created '" + file + "'");
            }
            outputFiles.add(file);
        } catch (IOException e) {
            throw new SystemErrorException(e);
        }
    }

}
