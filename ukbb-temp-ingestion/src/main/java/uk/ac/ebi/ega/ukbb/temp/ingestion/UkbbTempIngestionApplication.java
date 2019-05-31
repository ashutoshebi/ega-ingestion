package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IReEncryptService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@SpringBootApplication
public class UkbbTempIngestionApplication implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(UkbbTempIngestionApplication.class);

	// TODO bjuhasz: what permissions are needed for this Spring Boot application
	//  to be able to write into the STAGING_PATH directory?
	private static final Path STAGING_PATH = Paths.get("/nfs/ega/public/staging/ukbb-temp-ingestion/re-encrypted-files");

	private ApplicationContext applicationContext;

	private IReEncryptService reEncryptService;

	@Autowired
	public UkbbTempIngestionApplication(final ApplicationContext applicationContext,
			final IReEncryptService reEncryptService) {
		this.applicationContext = applicationContext;
		this.reEncryptService = reEncryptService;
	}

	@Override
	public void run(final String... args) {
		final CmdLineArgs cmdLineArgs = parseCommandLineArguments(args);

		final Result result = reRunReEncryptionIfNeeded(cmdLineArgs.inputFilePath,
				cmdLineArgs.inputPassword, cmdLineArgs.outputFilePath, cmdLineArgs.outputPassword);

		LOGGER.info("Result of re-encryption: message and exception: {}, status: {}, " +
						"startTime: {}, endTime: {}",
				result.getMessageAndException(), result.getStatus(),
				result.getStartTime(), result.getEndTime());
	}

	public static void main(final String... args) {
		SpringApplication.run(UkbbTempIngestionApplication.class, args);
	}

	private CmdLineArgs parseCommandLineArguments(final String... args) {
		assertCorrectNumberOfCommandLineArguments(args);

		final Path inputFilePath = Paths.get(args[0]);
		final String inputPassword = args[1];
		final Path outputFilePath;
		final String outputPassword;

		if (args.length == 3) {
			outputFilePath = getOutputFilePathInStagingBasedOn(inputFilePath);
			outputPassword = args[2];
		} else {
			outputFilePath = Paths.get(args[2]);
			outputPassword = args[3];
		}

		return new CmdLineArgs(inputFilePath, inputPassword, outputFilePath, outputPassword);
	}

	private void assertCorrectNumberOfCommandLineArguments(final String... args) {
		if (args.length < 3 || args.length > 4) {
			final String message = String.format("Either 3 or 4 arguments are needed:\n" +
					"INPUT_FILE INPUT_PASSWORD [OPTIONAL_OUTPUT_FILE] OUTPUT_PASSWORD\n\n" +
					"If the optional output file is not given, then the re-encrypted " +
					"output file will be placed into %s.", STAGING_PATH);
			LOGGER.error(message);

			// https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-application-exit
			System.exit(SpringApplication.exit(applicationContext, () -> 1));
		}
	}

	/**
	 * TODO bjuhasz: document this function
	 *
	 * @param inputFilePath
	 * @return
	 */
	private Path getOutputFilePathInStagingBasedOn(final Path inputFilePath) {
		final Path fileName = inputFilePath.getFileName();

		final String message = String.format("%s should contain a filename", inputFilePath);
		Objects.requireNonNull(fileName, message);

		return STAGING_PATH.resolve(fileName);
	}

	/**
	 * If the given input file has already been re-encrypted successfully,
	 * then just return the stored result of that successful re-encryption.
	 * Otherwise, just re-run the re-encryption again.
	 *
	 * @return the result of a previous successful re-encryption
	 *         or the result of the current re-encryption.
	 */
	private Result reRunReEncryptionIfNeeded(final Path inputFilePath, final String inputPassword,
											 final Path outputFilePath, final String outputPassword) {
		return reEncryptService
				.getReEncryptionResultFor(inputFilePath)
				.filter(r -> Result.Status.SUCCESS.equals(r.getStatus()))
				.orElse(reEncryptService.reEncrypt(inputFilePath, inputPassword, outputFilePath, outputPassword));
	}

	private class CmdLineArgs {
		Path inputFilePath;
		String inputPassword;
		Path outputFilePath;
		String outputPassword;

		CmdLineArgs(final Path inputFilePath, final String inputPassword,
					final Path outputFilePath, final String outputPassword) {
			this.inputFilePath = inputFilePath;
			this.inputPassword = inputPassword;
			this.outputFilePath = outputFilePath;
			this.outputPassword = outputPassword;
		}
	}
}
