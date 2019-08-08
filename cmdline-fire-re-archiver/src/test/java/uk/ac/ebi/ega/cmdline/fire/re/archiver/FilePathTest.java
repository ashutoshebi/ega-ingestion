package uk.ac.ebi.ega.cmdline.fire.re.archiver;

import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class FilePathTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePathTest.class);
    private static final String EXTENSION_OF_RE_ENCRYPTED_FILES = ".cip";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testCreatingNewFileExtension() throws IOException {
        final File originalFile = temporaryFolder.newFile("tempFileName.tmp");
        final String originalFileAbsolutePath = originalFile.getAbsolutePath();
        LOGGER.info("originalFileAbsolutePath: {} ", originalFileAbsolutePath);
        assertThat(originalFileAbsolutePath).endsWith("tempFileName.tmp");

        final File reEncryptedFile = getReEncryptedFileBasedOn(originalFile);

        final String reEncryptedFileAbsolutePath = reEncryptedFile.getAbsolutePath();
        LOGGER.info("reEncryptedFileAbsolutePath: {} ", reEncryptedFileAbsolutePath);
        assertThat(reEncryptedFileAbsolutePath).endsWith("tempFileName.cip");
    }

    private File getReEncryptedFileBasedOn(final File file) {
        final String absFilePathWithoutExtension = FilenameUtils.removeExtension(file.getAbsolutePath());
        final String absFilePathWithExtension = absFilePathWithoutExtension + EXTENSION_OF_RE_ENCRYPTED_FILES;
        return new File(absFilePathWithExtension);
    }
}

