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

@SpringBootApplication
public class UkbbTempIngestionApplication implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(UkbbTempIngestionApplication.class);

	private static final int EXPECTED_NUMBER_OF_ARGUMENTS = 3;

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
		assertCorrectNumberOfCommandLineArguments(args);

		final Path inputFilePath = Paths.get(args[0]);
		final String inputPassword = args[1];
		final Path outputFilePath = Paths.get("/tmp/output.txt"); // TODO bjuhasz
		final String outputPassword = args[2];

		final Result result = reEncryptService.reEncrypt(inputFilePath, inputPassword,
				outputFilePath, outputPassword);

		LOGGER.info("Result of re-encryption: message and exception: {}, status: {}, " +
						"startTime: {}, endTime: {}",
				result.getMessageAndException(), result.getStatus(),
				result.getStartTime(), result.getEndTime());
	}

	public static void main(final String... args) {
		SpringApplication.run(UkbbTempIngestionApplication.class, args);
	}

	private void assertCorrectNumberOfCommandLineArguments(final String... args) {
		if (args.length != EXPECTED_NUMBER_OF_ARGUMENTS) {
			final String message = "Exactly 3 arguments are needed:\n" +
					"[filename] [input password] [output password]";
			LOGGER.error(message);

			// https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-application-exit
			System.exit(SpringApplication.exit(applicationContext, () -> 1));
		}
	}

}
