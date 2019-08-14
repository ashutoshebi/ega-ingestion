package uk.ac.ebi.ega.fire.ingestion.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OldFireServiceTest {

    private static final String EGA_FILE_ID = "egaFileId";
    private static final String MD5 = "md5 of file-to-be-archived";
    private static final Long ARCHIVE_ID = 234L;

    private final IProFilerDatabaseService proFilerDatabaseService = mock(IProFilerDatabaseService.class);
    private IFireService fireService;
    private String pathOnFire;
    private File fileToBeArchived;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        pathOnFire = temporaryFolder.newFolder().getPath();
        fileToBeArchived = temporaryFolder.newFile();
        final Path fireStaging = temporaryFolder.newFolder().toPath();

        when(proFilerDatabaseService.archiveFile(eq(EGA_FILE_ID), any(File.class), eq(MD5), eq(pathOnFire)))
                .thenReturn(ARCHIVE_ID);

        fireService = new OldFireService(fireStaging, proFilerDatabaseService);
    }

    @Test
    public void archiveFile_SuppliedCorrectArguments_ExecutesSuccessfully() {
        final Optional<Long> archiveId = fireService.archiveFile(EGA_FILE_ID, fileToBeArchived, MD5, pathOnFire);

        assertThat(archiveId).isPresent().contains(ARCHIVE_ID);
    }

    @Test
    public void archiveFile_SuppliedNonExistentInputFile_ReturnsEmptyArchiveId() {
        final File nonExistentFile = new File("/does/not/exist");
        final Optional<Long> archiveId = fireService.archiveFile(EGA_FILE_ID, nonExistentFile, MD5, pathOnFire);

        assertThat(archiveId).isEmpty();
    }

    @Test
    public void archiveFile_SuppliedNonExistentDestination_ReturnsEmptyArchiveId() {
        final Optional<Long> archiveId = fireService.archiveFile(EGA_FILE_ID, fileToBeArchived,
                MD5, "/nonExistentPath/on/fire");

        assertThat(archiveId).isEmpty();
    }
}