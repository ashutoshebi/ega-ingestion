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

import uk.ac.ebi.ega.encryption.core.EncryptionOutputStream;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class IngestionSamToolsIndex extends DefaultIngestionPipeline {

    private File index;

    public IngestionSamToolsIndex(File origin, File secretRing, File passphrase, File output,
                                  File index, char[] password) {
        super(origin, secretRing, passphrase, output, password);
        this.index = index;
    }

    @Override
    protected void doProcess() throws SystemErrorException {
        Process process = startSamToolsProcess();
        try (
                final EncryptionOutputStream encryptionOutputStream = getEncryptionOutputStream();
                final OutputStream samToolsInput = process.getOutputStream();
        ) {
            executePipeline(encryptionOutputStream, samToolsInput);
            waitForSuccessfulProcessEnd(process);
        } catch (IOException e) {
            throw new SystemErrorException(e);
        } catch (SystemErrorException | UserErrorException e) {
            terminateProcess(process);
        }
    }

    private void terminateProcess(Process process) {
        if (process.isAlive()) {
            process.destroy();
        }
    }

    private void waitForSuccessfulProcessEnd(Process process) throws SystemErrorException {
        try {
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                throw new SystemErrorException("Samtool process did not finish");
            }
            if (process.exitValue() != 0) {
                throw new SystemErrorException("Index could not be generated '");
            }
        } catch (InterruptedException e) {
            throw new SystemErrorException("Samtool process did not finish");
        }
    }

    private Process startSamToolsProcess() throws SystemErrorException {
        ProcessBuilder pb = new ProcessBuilder("samtools", "index", "-", "-");
        pb.redirectOutput(index);
        try {
            return pb.start();
        } catch (IOException e) {
            throw new SystemErrorException(e);
        }
    }

    @Override
    protected void deleteOutputFiles() throws SystemErrorException {
        super.deleteOutputFiles();
        createFiles();
    }

    @Override
    protected void createOutputFiles() throws SystemErrorException {
        super.createOutputFiles();
        deleteFiles();
    }
}
