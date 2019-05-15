/*
 *
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
 *
 */
package uk.ac.ebi.ega.file.re.encryption.processor.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Job;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.exceptions.JobRetryException;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.fire.IFireFile;
import uk.ac.ebi.ega.fire.IFireService;
import uk.ac.ebi.ega.fire.exceptions.FireConfigurationException;
import uk.ac.ebi.ega.fire.exceptions.MaxRetryOnConnectionReached;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Objects;

public class ReEncryptJob implements Job<ReEncryptJobParameters> {

    private static final Logger logger = LoggerFactory.getLogger(ReEncryptJob.class);

    private IFireService fireService;

    private char[] password;

    public ReEncryptJob(IFireService fireService, char[] password) {
        this.fireService = fireService;
        this.password = password;
    }

    @Override
    public Result execute(ReEncryptJobParameters parameters) {
        LocalDateTime start = LocalDateTime.now();
        try {
            final IFireFile inputFile = fireService.getFile(parameters.getDosId());
            final File outputFile = getOutputFile(parameters.getResultPath());

            char[] newPassword;
            try {
                newPassword = parameters.getPassword();
            } catch (AlgorithmInitializationException e) {
                return Result.abort("Password could not be decrypted correctly", e, start);
            }

            try (DecryptInputStream decryptStream = new DecryptInputStream(inputFile.getStream(), new AesCtr256Ega(),
                    password);
                 EncryptOutputStream encryptOutputStream = new EncryptOutputStream(new FileOutputStream(outputFile),
                         new AesCtr256Ega(), newPassword)
            ) {
                logger.info("File size {}", FileUtils.normalizeSize(inputFile.getSize()));
                byte[] buffer = new byte[8192];
                int bytesRead = decryptStream.read(buffer);
                while (bytesRead != -1) {
                    encryptOutputStream.write(buffer, 0, bytesRead);
                    bytesRead = decryptStream.read(buffer);
                }

                if (!Objects.equals(inputFile.getMd5(), decryptStream.getMd5())) {
                    final String format = String.format("Mismatch of md5: expected %s actual %s", inputFile.getMd5(),
                            decryptStream.getMd5());
                    throw new JobRetryException(format);
                }
            }
        } catch (SocketTimeoutException | MaxRetryOnConnectionReached e) {
            throw new JobRetryException("Fire is currently down");
        } catch (FileNotFoundException e) {
            return Result.failure("File could not be found on DOS", e, start);
        } catch (AlgorithmInitializationException e) {
            return Result.failure("Error while decrypting the file on DOS", e, start);
        } catch (FireConfigurationException | ParseException | IOException e) {
            return Result.abort("Unrecoverable error", e, start);
        }
        return Result.correct(start);
    }

    private File getOutputFile(String resultPath) throws IOException {
        final Path path = Paths.get(resultPath);
        Files.createDirectories(path.getParent());
        File outputFile;
        try {
            outputFile = Files.createFile(path).toFile();
        } catch (FileAlreadyExistsException e) {
            outputFile = path.toFile();
            logger.warn("File {} already exists, process will overwrite the file", outputFile);
        }
        return outputFile;
    }

}
