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
package uk.ac.ebi.ega.file.re.encryption.processor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.listeners.IngestionEventListener;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptResult;
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
import java.util.Objects;

public class ReEncryptService implements IReEncryptService {

    private final Logger logger = LoggerFactory.getLogger(IngestionEventListener.class);

    private IFireService fireService;

    private String passwordFile;

    private IMailingService mailingService;

    private String reportTo;

    public ReEncryptService(IFireService fireService, String passwordFile, IMailingService mailingService,
                            String reportTo) {
        this.fireService = fireService;
        this.passwordFile = passwordFile;
        this.mailingService = mailingService;
        this.reportTo = reportTo;
    }

    @Override
    public ReEncryptResult reEncrypt(String dosId, String resultPath, char[] resultPassword) throws ParseException, IOException, FireConfigurationException {
        try {
            char[] password = FileUtils.readPasswordFile(Paths.get(passwordFile));
            final IFireFile inputFile = fireService.getFile(dosId);
            final File outputFile = getOutputFile(resultPath);

            try (DecryptInputStream decryptStream = new DecryptInputStream(inputFile.getStream(), new AesCtr256Ega(),
                    password);
                 EncryptOutputStream encryptOutputStream = new EncryptOutputStream(new FileOutputStream(outputFile),
                         new AesCtr256Ega(), resultPassword);
            ) {
                logger.info("File size {}", FileUtils.normalizeSize(inputFile.getSize()));
                byte[] buffer = new byte[8192];
                int bytesRead = decryptStream.read(buffer);
                while (bytesRead != -1) {
                    encryptOutputStream.write(buffer, 0, bytesRead);
                    bytesRead = decryptStream.read(buffer);
                }

                if (!Objects.equals(inputFile.getMd5(), decryptStream.getMd5())) {
                    return retry("Mismatch of md5: expected %s actual %s", inputFile.getMd5(), decryptStream.getMd5());
                }
            }
        } catch (SocketTimeoutException | MaxRetryOnConnectionReached e) {
            return retry("Fire is currently down");
        } catch (FileNotFoundException e) {
            return error("File could not be found on DOS", e);
        } catch (AlgorithmInitializationException e) {
            return error("Error while decrypting the file on DOS", e);
        } catch (FireConfigurationException | ParseException | IOException e) {
            reportError("Unrecoverable error", e);
            throw e;
        }
        return new ReEncryptResult(ReEncryptResult.Status.CORRECT, null);
    }

    private ReEncryptResult error(String msg, Exception e) {
        reportError(msg, e);
        return new ReEncryptResult(msg, e);
    }

    private ReEncryptResult retry(String msg, Object... objects) {
        final String formattedMessage = String.format(msg, objects);
        logger.warn(formattedMessage);
        return new ReEncryptResult(ReEncryptResult.Status.RETRY, formattedMessage);
    }

    private void reportError(String message, Exception e) {
        logger.error(message, e);
        mailingService.sendSimpleMessage(reportTo, message, e);
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
