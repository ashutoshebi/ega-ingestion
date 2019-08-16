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
package uk.ac.ebi.ega.cmdline.fire.re.archiver;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.services.IReEncryptionService;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.services.IngestionPipelineResult;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.services.exceptions.SystemErrorException;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.services.exceptions.UserErrorException;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.utils.IStableIdGenerator;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class CmdLineFireReArchiverCommandLineRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdLineFireReArchiverCommandLineRunner.class);

    private static final String EXTENSION_OF_RE_ENCRYPTED_FILES = ".cip";

    private final ApplicationContext applicationContext;
    private final IFireService fireService;
    private final IStableIdGenerator stableIdGenerator;
    private final IReEncryptionService reEncryptionService;

    CmdLineFireReArchiverCommandLineRunner(final ApplicationContext applicationContext,
                                           final IFireService fireService,
                                           final IStableIdGenerator stableIdGenerator,
                                           final IReEncryptionService reEncryptionService) {
        this.applicationContext = applicationContext;
        this.fireService = fireService;
        this.stableIdGenerator = stableIdGenerator;
        this.reEncryptionService = reEncryptionService;
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
        try {
            LOGGER.debug("Received the following command-line arguments: {}", args);

            final String stableId = stableIdGenerator.generate();
            final File inputFile = args.getFilePath().toFile();
            final File reEncryptedFile = getOutputFileBasedOn(inputFile, EXTENSION_OF_RE_ENCRYPTED_FILES);
            final String pathOnFire = args.getPathOnFire();

            LOGGER.trace("Re-encrypting the {} input-file into {}...", inputFile, reEncryptedFile);
            final IngestionPipelineResult ingestionPipelineResult = reEncryptionService.reEncrypt(inputFile, reEncryptedFile);
            LOGGER.trace("The result of re-encrypting the {} input-file: {}", inputFile, ingestionPipelineResult);

            final String reEncryptedMd5 = ingestionPipelineResult.getEncryptedFile().getMd5();

            LOGGER.trace("Archiving the re-encrypted file {} with stableId {} into {}...",
                    reEncryptedFile, stableId, pathOnFire);
            final Optional<Long> optionalArchiveId = fireService.archiveFile(stableId, reEncryptedFile,
                    reEncryptedMd5, pathOnFire);
            LOGGER.debug("archiveId: {}", optionalArchiveId);

            return ReturnValue.EVERYTHING_OK.getValue();

        } catch (UserErrorException | SystemErrorException e) {
            LOGGER.error("Exception during re-encryption: ", e);
            return ReturnValue.EXCEPTION_DURING_RE_ENCRYPTION.getValue();
        }
    }

    /**
     * Creates a File object (called outputFile) based on the inputFile:
     * the outputFile is located in the same directory as the inputFile,
     * but it will have a different file extension: the one which was given.
     *
     * @param inputFile a File
     * @param extension the outputFile will have this extension,
     *                  instead of the inputFile's extension
     * @return a File which is located in the same directory as the inputFile,
     * but which has the supplied file extension.
     */
    private File getOutputFileBasedOn(final File inputFile, final String extension) {
        final String absFilePathWithoutExtension = FilenameUtils.removeExtension(inputFile.getAbsolutePath());
        final String absFilePathWithExtension = absFilePathWithoutExtension + extension;
        return new File(absFilePathWithExtension);
    }

    enum ReturnValue {
        EVERYTHING_OK(0),
        EXCEPTION_DURING_RE_ENCRYPTION(1),
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
