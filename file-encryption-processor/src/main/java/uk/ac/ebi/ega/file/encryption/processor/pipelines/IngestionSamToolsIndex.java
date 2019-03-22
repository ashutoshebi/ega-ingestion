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
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.stream.PipelineStream;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class IngestionSamToolsIndex extends DefaultIngestionPipeline {

    private File index;

    private File indexError;

    public IngestionSamToolsIndex(File origin, File secretRing, File passphrase, File output,
                                  File index, char[] password) {
        super(origin, secretRing, passphrase, output, password);
        this.index = index;
        this.indexError = new File(index.getAbsolutePath() + ".error");
    }

    @Override
    protected IngestionPipelineResult doProcess() throws SystemErrorException, UserErrorException, IOException {
        createFile(indexError);
        CmdLineProcessPiped process = new SamIndexProcessPiped(indexError);
        try (
                final DecryptInputStream decryptInputStream = getDecryptionInputStream();
                final EncryptOutputStream encryptOutputStream = getEncryptionOutputStream(output);
                final EncryptOutputStream encryptIndexOutputStream = getEncryptionOutputStream(index);
                final PipelineStream stream = StreamPipelineBuilder
                        .source(decryptInputStream)
                        .to(encryptOutputStream)
                        .to(process.getPipeToProcess())
                        .build();
        ) {
            process.pipeOutputTo(encryptIndexOutputStream);
            stream.execute();
            stream.close();
            waitForSuccessfulProcessEnd(process);
            indexError.delete();
            return new IngestionPipelineResult(
                    new IngestionPipelineFile(origin, decryptInputStream.getMd5()),
                    decryptInputStream.getUnencryptedMd5(),
                    password,
                    new IngestionPipelineFile(output, encryptOutputStream.getMd5()),
                    new IngestionPipelineFile(index, encryptIndexOutputStream.getMd5())
            );
        } catch (SystemErrorException e) {
            process.terminateProcess();
            throw e;
        } catch (AlgorithmInitializationException e) {
            throw new UserErrorException(e);
        }
    }

    private void waitForSuccessfulProcessEnd(CmdLineProcessPiped process) throws SystemErrorException,
            UserErrorException {
        if (!process.waitFor(10, TimeUnit.MINUTES)) {
            throw new SystemErrorException("Samtool process did not finish");
        }
        if (process.exitValue() != 0) {
            try {
                throw new UserErrorException("Index could not be generated, reason '" +
                        process.getErrorRedirectLog() + "'");
            } catch (IOException e) {
                //This should never happen
                throw new SystemErrorException(e);
            }
        }
    }
}
