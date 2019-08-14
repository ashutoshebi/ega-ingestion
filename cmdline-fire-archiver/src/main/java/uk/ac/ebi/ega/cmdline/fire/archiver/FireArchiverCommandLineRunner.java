/*
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
 */
package uk.ac.ebi.ega.cmdline.fire.archiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import uk.ac.ebi.ega.cmdline.fire.archiver.utils.IStableIdGenerator;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class FireArchiverCommandLineRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FireArchiverCommandLineRunner.class);

    private static final int BUFFER_SIZE = 8192;

    private final ApplicationContext applicationContext;
    private final IFireService fireService;
    private final IStableIdGenerator stableIdGenerator;

    FireArchiverCommandLineRunner(final ApplicationContext applicationContext,
                                  final IFireService fireService,
                                  final IStableIdGenerator stableIdGenerator) {
        this.applicationContext = applicationContext;
        this.fireService = fireService;
        this.stableIdGenerator = stableIdGenerator;
    }

    @Override
    public void run(final String... args) throws IOException {
        final Optional<CommandLineParser> optionalParsedArgs = CommandLineParser.parse(args);

        System.exit(SpringApplication.exit(applicationContext,
                () -> optionalParsedArgs
                        .map(this::archiveFile)
                        .orElse(ReturnValue.EMPTY_COMMAND_LINE_ARGUMENTS.getValue())));
    }

    int archiveFile(final CommandLineParser args) {
        LOGGER.debug("Received the following command-line arguments: {}", args);

        try {
            final String stableId = stableIdGenerator.generate();
            final File inputFile = args.getFilePath().toFile();
            final String md5 = getMd5(inputFile);
            final String pathOnFire = args.getPathOnFire();

            final Optional<Long> optionalArchiveId = fireService.archiveFile(stableId, inputFile, md5, pathOnFire);
            LOGGER.debug("archiveId: {}", optionalArchiveId);

            return ReturnValue.EVERYTHING_OK.getValue();
        } catch (FileNotFoundException e) {
            LOGGER.error("Input file was not found: {}", e.getMessage());
            return ReturnValue.EXCEPTION_DURING_ARCHIVING.getValue();
        } catch (NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Exception during archiving: ", e);
            return ReturnValue.EXCEPTION_DURING_ARCHIVING.getValue();
        }
    }

    private String getMd5(final File inputFile) throws IOException, NoSuchAlgorithmException {
        try (final InputStream encryptedInput = new FileInputStream(inputFile)) {

            final byte[] buffer = new byte[BUFFER_SIZE];
            final MessageDigest messageDigestOfEncryptedFile = MessageDigest.getInstance("MD5");
            int bytesRead;

            do {
                bytesRead = encryptedInput.read(buffer);
                if (bytesRead > 0) {
                    messageDigestOfEncryptedFile.update(buffer, 0, bytesRead);
                }
            } while (bytesRead != -1);

            final String md5 = Hash.normalize(messageDigestOfEncryptedFile);

            LOGGER.debug("The MD5 of the {} file is: {}", inputFile, md5);

            return md5;
        }
    }

    enum ReturnValue {
        EVERYTHING_OK(0),
        EXCEPTION_DURING_ARCHIVING(1),
        EMPTY_COMMAND_LINE_ARGUMENTS(2);

        private int value;

        ReturnValue(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
