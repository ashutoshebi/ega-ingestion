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

import uk.ac.ebi.ega.encryption.core.DecryptionInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptionOutputStream;
import uk.ac.ebi.ega.encryption.core.StreamPipelineBuilder;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.stream.StreamSource;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DefaultIngestionPipeline implements IngestionPipeline {

    private File origin;

    private File secretRing;

    private File passphrase;

    private File output;

    private File outputPassword;

    private char[] password;

    public DefaultIngestionPipeline(File origin, File secretRing, File passphrase, File output, char[] password) {
        this.origin = origin;
        this.secretRing = secretRing;
        this.passphrase = passphrase;
        this.output = output;
        this.outputPassword = new File(output.getAbsolutePath() + ".password");
    }

    @Override
    public void process() throws SystemErrorException, UserErrorException {
        try {
            createOutputFiles();
            doProcess();
        } catch (SystemErrorException | UserErrorException e) {
            deleteOutputFiles();
            throw e;
        }
    }

    protected void doProcess() throws SystemErrorException, UserErrorException {
        try (
                final EncryptionOutputStream encryptionOutputStream = getEncryptionOutputStream();
        ) {
            executePipeline(encryptionOutputStream);
        } catch (IOException e) {
            throw new SystemErrorException(e);
        }
    }

    protected final void executePipeline(OutputStream... outputStreams) throws SystemErrorException,
            UserErrorException {
        try (
                final DecryptionInputStream decryptionInputStream = getDecryptionInputStream();
                StreamSource source = StreamPipelineBuilder.source(decryptionInputStream).to(outputStreams);
        ) {
            source.execute();
        } catch (IOException e) {
            throw new SystemErrorException(e);
        } catch (AlgorithmInitializationException e) {
            throw new UserErrorException(e);
        }
    }

    private DecryptionInputStream getDecryptionInputStream() throws AlgorithmInitializationException, IOException {
        return new DecryptionInputStream(
                new FileInputStream(origin),
                new PgpKeyring(new FileInputStream(secretRing)),
                FileUtils.readPasswordFile(passphrase.toPath()));
    }

    protected EncryptionOutputStream getEncryptionOutputStream() throws SystemErrorException {
        try {
            return new EncryptionOutputStream(new FileOutputStream(output), new AesCtr256Ega(), password);
        } catch (Exception e) {
            throw new SystemErrorException(e);
        }
    }

    protected void deleteOutputFiles() throws SystemErrorException {
        deleteFiles(output, outputPassword);
    }

    protected void createOutputFiles() throws SystemErrorException {
        createFiles(output, outputPassword);
    }

    protected static void createFiles(File... files) throws SystemErrorException {
        for (File file : files) {
            try {
                if (!file.createNewFile()) {
                    throw new SystemErrorException("File could not be created '" + file + "'");
                }
            } catch (IOException e) {
                throw new SystemErrorException(e);
            }
        }
    }

    protected static void deleteFiles(File... files) throws SystemErrorException {
        for (File file : files) {
            if (file.exists() && !file.delete()) {
                throw new SystemErrorException("File could not be deleted");
            }
        }
    }

}
