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
package uk.ac.ebi.ega.file.encryption.processor.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.listener.EncryptEventListener;
import uk.ac.ebi.ega.file.encryption.processor.service.FileEncryptionService;
import uk.ac.ebi.ega.file.encryption.processor.service.IFileEncryptionService;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionData;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Configuration
public class FileEncryptionConfiguration {

    @Bean
    public IPasswordEncryptionService passwordEncryptionService(@Value("${file.encryption.key.password.path}") String path)
            throws IOException {
        return new PasswordEncryptionService(FileUtils.readPasswordFile(Paths.get(path)));
    }

    @Bean
    public EncryptEventListener initFileEncryptionEventListener(final IFileEncryptionService fileEncryptionProcessor,
                                                                final KafkaTemplate<String, FileEncryptionResult> kafkaTemplate,
                                                                @Value("${spring.kafka.file.archive.queue.name}") String completedTopic) {
        return new EncryptEventListener(fileEncryptionProcessor, kafkaTemplate, completedTopic);
    }

    @Bean
    public IFileEncryptionService initFileEncryptionProcessor(IPasswordEncryptionService passwordEncryptionService,
                                                              @Value("${file.encryption.keyring.private}") String privateKeyRing,
                                                              @Value("${file.encryption.keyring.private.key}") String privateKeyRingPassword,
                                                              @Value("${file.encryption.output.path}") String outputFolder) throws IOException {
        final File privateKeyRingFile = new File(privateKeyRing);
        final File privateKeyRingPasswordFile = new File(privateKeyRingPassword);
        assertFiles(privateKeyRingFile, privateKeyRingPasswordFile);
        final Path outputFolderPath = Paths.get(outputFolder);
        assertWritePermissionsInOutputFolder(outputFolderPath);
        return new FileEncryptionService(passwordEncryptionService, privateKeyRingFile, privateKeyRingPasswordFile,
                outputFolderPath);
    }

    private void assertFiles(File privateKeyRingFile, File privateKeyRingPasswordFile) throws FileNotFoundException {
        if (!privateKeyRingFile.exists()) {
            throw new FileNotFoundException("Private key ring file could not be found");
        }

        if (!privateKeyRingPasswordFile.exists()) {
            throw new FileNotFoundException("Password file for private key ring could not be found");
        }
    }

    private void assertWritePermissionsInOutputFolder(Path outputFolderPath) throws FileNotFoundException, FileSystemException {
        if (!outputFolderPath.toFile().exists()) {
            throw new FileNotFoundException("Output file path ".
                    concat(outputFolderPath.toAbsolutePath().toString()).concat(" doesn't not exists!"));
        }
        final File testFile = new File(outputFolderPath.resolve(UUID.randomUUID().toString()).toAbsolutePath().toString());
        try (final FileOutputStream fileOutputStream = new FileOutputStream(testFile)) {
            fileOutputStream.write("Data can be written to File".getBytes());
            fileOutputStream.flush();
        } catch (IOException e) {
            throw new FileSystemException("Unable to write file inside Output folder path ".concat(testFile.getAbsolutePath()));
        }

        if (!testFile.delete()) {
            throw new FileSystemException("Unable to delete file inside Output folder path ".concat(testFile.getAbsolutePath()));
        }
    }

}
