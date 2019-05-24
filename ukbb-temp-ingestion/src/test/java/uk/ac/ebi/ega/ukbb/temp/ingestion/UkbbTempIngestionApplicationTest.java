package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;

import static org.assertj.core.api.Assertions.assertThat;

public class UkbbTempIngestionApplicationTest {

	private static final String FILE_NAME = "fileName";
	private static final String INPUT_PASSWORD = "inputPassword";
	private static final String OUTPUT_PASSWORD = "outputPassword";

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void main_SuppliedCorrectArguments_ReturnsSuccess() {
		final String[] commandLineArgs = new String[] { FILE_NAME, INPUT_PASSWORD, OUTPUT_PASSWORD };

		UkbbTempIngestionApplication.main(commandLineArgs);

		final String resultOfReEncryption = outputCapture.toString();
		assertThat(resultOfReEncryption).contains("status: " + Result.Status.SUCCESS);
	}

	// TODO bjuhasz
	@Ignore("I think this test needs an ApplicationContext")
	@Test
	public void main_SuppliedTooFewArguments_ReturnsFailure() {
		final String[] commandLineArgs = new String[] { FILE_NAME, INPUT_PASSWORD };

		UkbbTempIngestionApplication.main(commandLineArgs);

		final String resultOfReEncryption = outputCapture.toString();
		assertThat(resultOfReEncryption).contains("status: " + Result.Status.FAILURE);
	}

}
