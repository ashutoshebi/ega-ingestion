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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.ApplicationContext;
import uk.ac.ebi.ega.cmdline.fire.archiver.FireArchiverCommandLineRunner.ReturnValue;
import uk.ac.ebi.ega.cmdline.fire.archiver.utils.IStableIdGenerator;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FireArchiverCommandLineRunnerTest {

    private static final String STABLE_ID = "CMD_123";
    private static final String MD5_OF_INPUT_FILE = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String PATH_ON_FIRE = "/fire/path/store/here";
    private static final Long ARCHIVE_ID = 234L;

    private final IFireService fireService = mock(IFireService.class);
    private final IStableIdGenerator stableIdGenerator = mock(IStableIdGenerator.class);
    private final ApplicationContext applicationContext = mock(ApplicationContext.class);

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final FireArchiverCommandLineRunner archiver = new FireArchiverCommandLineRunner(applicationContext,
            fireService, stableIdGenerator);

    private File inputFile;

    @Before
    public void setUp() throws IOException {
        inputFile = temporaryFolder.newFile();
        when(stableIdGenerator.generate()).thenReturn(STABLE_ID);
    }

    @Test
    public void commandLineRunner_SuppliedCorrectArguments_ExecutesSuccessfully() throws IOException {
        when(fireService.archiveFile(eq(STABLE_ID), eq(inputFile), eq(MD5_OF_INPUT_FILE), eq(PATH_ON_FIRE)))
                .thenReturn(Optional.of(ARCHIVE_ID));
        final CommandLineParser correctArguments = getCorrectArguments();

        final int returnValue = archiver.archiveFile(correctArguments);

        assertThat(returnValue).isEqualTo(ReturnValue.EVERYTHING_OK.ordinal());
        verify(fireService).archiveFile(eq(STABLE_ID), eq(inputFile), eq(MD5_OF_INPUT_FILE), eq(PATH_ON_FIRE));
    }

    @Test
    public void commandLineRunner_SuppliedWithNonExistingInputFile_ReturnsFailureReturnValue() throws IOException {
        final CommandLineParser invalidArguments = getArguments("/non/existent/file");

        final int returnValue = archiver.archiveFile(invalidArguments);

        assertThat(returnValue).isEqualTo(ReturnValue.EXCEPTION_DURING_ARCHIVING.ordinal());
        verify(fireService, never()).archiveFile(anyString(), any(File.class), anyString(), anyString());
    }

    private CommandLineParser getArguments(final String inputFilePath) throws IOException {
        final String filePathArg = String.format("--filePath=%s", inputFilePath);
        final String pathOnFireArg = String.format("--pathOnFire=%s", PATH_ON_FIRE);

        return CommandLineParser.parse(filePathArg, pathOnFireArg).orElseThrow(AssertionError::new);
    }

    private CommandLineParser getCorrectArguments() throws IOException {
        final String inputFilePath = inputFile.toPath().toString();
        return getArguments(inputFilePath);
    }

}
