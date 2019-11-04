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
package uk.ac.ebi.ega.file.encryption.processor.pipeline;

import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.StreamPipelineBuilder;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.stream.PipelineStream;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultEncryptionPipeline {

    private final List<File> outputFiles;
    private File secretRing;
    private File secretRingKey;
    protected File origin;
    protected File output;
    protected char[] newEncryptionKey;

    public DefaultEncryptionPipeline(final File origin, final File secretRing, final File secretRingKey,
                                     final File output, final char[] newEncryptionKey) {
        this.outputFiles = new ArrayList<>();
        this.origin = origin;
        this.secretRing = secretRing;
        this.secretRingKey = secretRingKey;
        this.output = output;
        this.newEncryptionKey = newEncryptionKey;
    }

    public final IngestionPipelineResult process() throws IOException, AlgorithmInitializationException {
        try (
                final DecryptInputStream decryptInputStream = getDecryptionInputStream();
                final EncryptOutputStream encryptOutputStream = getEncryptionOutputStream(output);
                final PipelineStream stream = StreamPipelineBuilder
                        .source(decryptInputStream)
                        .to(encryptOutputStream)
                        .build()
        ) {
            long bytesTransferred = stream.execute();
            return new IngestionPipelineResult(
                    new IngestionPipelineFile(origin, decryptInputStream.getMd5(), decryptInputStream.available()),
                    decryptInputStream.getUnencryptedMd5(),
                    bytesTransferred,
                    new IngestionPipelineFile(output, encryptOutputStream.getMd5(), output.length())
            );
        }
    }

    protected final DecryptInputStream getDecryptionInputStream() throws AlgorithmInitializationException, IOException {
        return new DecryptInputStream(
                new FileInputStream(origin),
                new PgpKeyring(new FileInputStream(secretRing)),
                FileUtils.readPasswordFile(secretRingKey.toPath()));
    }

    protected final EncryptOutputStream getEncryptionOutputStream(final File file) throws IOException, AlgorithmInitializationException {
        createFile(file);
        return new EncryptOutputStream(new FileOutputStream(file), new AesCtr256Ega(), newEncryptionKey);
    }

    protected final void createFile(final File file) throws IOException {
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("File could not be created '" + file + "'");
        }
        outputFiles.add(file);
    }
}
