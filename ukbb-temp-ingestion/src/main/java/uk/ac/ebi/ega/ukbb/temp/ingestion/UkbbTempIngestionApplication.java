package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.ReEncryptService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class UkbbTempIngestionApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UkbbTempIngestionApplication.class);

    private ApplicationContext applicationContext;

    private ReEncryptService reEncryptService;

    @Autowired
    public UkbbTempIngestionApplication(final ApplicationContext applicationContext,
                                        final ReEncryptService reEncryptService) {
        this.applicationContext = applicationContext;
        this.reEncryptService = reEncryptService;
    }

    @Override
    public void run(final String... args) throws IOException {
        assertCorrectNumberOfCommandLineArguments(args);

        final Path inputFilePath = Paths.get(args[0]);
        final String inputPassword = readPasswordFromFile(args[1]);
        final String outputPassword = readPasswordFromFile(args[2]);

        final Result result = reRunReEncryptionIfNeeded(inputFilePath,
                inputPassword, outputPassword);

        LOGGER.info("Result of re-encryption: message and exception: {}, status: {}, " +
                        "startTime: {}, endTime: {}",
                result.getMessageAndException(), result.getStatus(),
                result.getStartTime(), result.getEndTime());
    }

    public static void main(final String... args) {
        SpringApplication.run(UkbbTempIngestionApplication.class, args);
    }

    private void assertCorrectNumberOfCommandLineArguments(final String... args) {
        if (args.length != 3) {
            final String message = "3 arguments are needed:\n" +
                    "INPUT_FILE_PATH INPUT_PASSWORD_FILE_PATH OUTPUT_PASSWORD_FILE_PATH";
            LOGGER.error(message);

            // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-application-exit
            System.exit(SpringApplication.exit(applicationContext, () -> 1));
        }
    }

    private String readPasswordFromFile(final String fileName) throws IOException {
        try {
            return Files.readAllLines(Paths.get(fileName)).get(0);
        } catch (IOException | IndexOutOfBoundsException e) {
            final String message = String.format("Could not read the password from file %s. " +
                    "%s should contain a password as its first line.", fileName, fileName);
            LOGGER.error(message, e);
            throw e;
        }
    }

    /**
     * If the given input file has already been re-encrypted successfully,
     * then just return the stored result of that successful re-encryption.
     * Otherwise, just re-run the re-encryption again.
     *
     * @return the result of a previous successful re-encryption
     *         or the result of the current re-encryption.
     */
    private Result reRunReEncryptionIfNeeded(final Path inputFilePath,
                                             final String inputPassword,
                                             final String outputPassword) {
        return reEncryptService
                .getReEncryptionResultFor(inputFilePath)
                .filter(r -> Result.Status.SUCCESS.equals(r.getStatus()))
                .orElse(reEncryptService.reEncryptAndStoreInProFiler(inputFilePath, inputPassword, outputPassword));
    }
}
