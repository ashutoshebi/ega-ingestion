package uk.ac.ebi.ega.ukbb.temp.ingestion;

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
	public void ukbbTempIngestionApplication_main_Runs_And_ReturnsFailureAsResult() {
		UkbbTempIngestionApplication.main(new String[] { FILE_NAME, INPUT_PASSWORD, OUTPUT_PASSWORD });

		final String resultOfReEncryption = outputCapture.toString();

		assertThat(resultOfReEncryption).contains("status: " + Result.Status.FAILURE);
	}

}
