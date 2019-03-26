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
package uk.ac.ebi.ega.file.encryption.processor.pipelines;

import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.StreamPipelineBuilder;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongPassword;
import uk.ac.ebi.ega.encryption.core.stream.PipelineStream;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultIngestionPipeline implements IngestionPipeline {

    private final List<File> outputFiles;

    protected File origin;

    private File secretRing;

    private File secretRingKey;

    protected File output;

    protected char[] password;

    public DefaultIngestionPipeline(File origin, File secretRing, File secretRingKey, File output, char[] password) {
        this.outputFiles = new ArrayList<>();
        this.origin = origin;
        this.secretRing = secretRing;
        this.secretRingKey = secretRingKey;
        this.output = output;
        this.password = password;
    }

    @Override
    public final IngestionPipelineResult process() throws SystemErrorException, UserErrorException {
        try {
            return doProcess();
        } catch (SystemErrorException | UserErrorException e) {
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

    protected IngestionPipelineResult doProcess() throws SystemErrorException, UserErrorException, IOException,
            AlgorithmInitializationException {
        try (
                final DecryptInputStream decryptInputStream = getDecryptionInputStream();
                final EncryptOutputStream encryptOutputStream = getEncryptionOutputStream(output);
                final PipelineStream stream = StreamPipelineBuilder
                        .source(decryptInputStream)
                        .to(encryptOutputStream)
                        .build();
        ) {
            stream.execute();
            return new IngestionPipelineResult(
                    new IngestionPipelineFile(origin, decryptInputStream.getMd5()),
                    decryptInputStream.getUnencryptedMd5(),
                    password,
                    new IngestionPipelineFile(output, encryptOutputStream.getMd5())
            );
        }
    }

    protected final DecryptInputStream getDecryptionInputStream() throws AlgorithmInitializationException, IOException {
        return new DecryptInputStream(
                new FileInputStream(origin),
                new PgpKeyring(new FileInputStream(secretRing)),
                FileUtils.readPasswordFile(secretRingKey.toPath()));
    }

    protected final EncryptOutputStream getEncryptionOutputStream(File file) throws SystemErrorException {
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

    protected final void createFile(File file) throws SystemErrorException {
        try {
            if (!file.createNewFile()) {
                throw new SystemErrorException("File could not be created '" + file + "'");
            }
            outputFiles.add(file);
        } catch (IOException e) {
            throw new SystemErrorException(e);
        }
    }

}
