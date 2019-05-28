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
package uk.ac.ebi.ega.ukbb.temp.ingestion.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.EncryptionReport;
import uk.ac.ebi.ega.encryption.core.Md5Check;
import uk.ac.ebi.ega.encryption.core.encryption.AesCbcOpenSSL;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.exceptions.Md5CheckException;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class ReEncryptService implements IReEncryptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReEncryptService.class);

    private static final int BUFFER_SIZE = 8192;

    @Override
    public Result reEncrypt(String inputFilePathAndName, String inputPassword, String outputPassword) {

        final LocalDateTime start = LocalDateTime.now();

        final File inputFile = new File(inputFilePathAndName);
        final File outputFile = new File("TODO bjuhasz");

        final String md5FromDatabase = "TODO bjuhasz";
        final Md5Check md5Check = Md5Check.any(md5FromDatabase);

        MessageDigest messageDigestEncrypted = Hash.getMd5();

        try (InputStream base64DecodedInputStream = Base64.getMimeDecoder().wrap(new FileInputStream(inputFile));
             DecryptInputStream decryptedStream = new DecryptInputStream(base64DecodedInputStream,
                     new AesCbcOpenSSL(),
                     inputPassword.toCharArray());

             EncryptOutputStream encryptedOutputStream = new EncryptOutputStream(new FileOutputStream(outputFile),
                     new AesCtr256Ega(),
                     outputPassword.toCharArray())) {

            long unencryptedSize = IOUtils.bufferedPipe(decryptedStream, encryptedOutputStream, BUFFER_SIZE);
            encryptedOutputStream.flush();

            String originalMd5 = Hash.normalize(messageDigestEncrypted);
            String unencryptedMd5 = decryptedStream.getUnencryptedMd5();
            md5Check.check(originalMd5, unencryptedMd5);

            final EncryptionReport encryptionReport = new EncryptionReport(originalMd5,
                    unencryptedMd5,
                    encryptedOutputStream.getMd5(),
                    unencryptedSize);

        } catch (FileNotFoundException e) {
            return Result.failure("File could not be found on DOS", e, start);
        } catch (AlgorithmInitializationException e) {
            return Result.failure("Error while decrypting the file on DOS", e, start);
        } catch (Md5CheckException e) {
            return Result.failure("Mismatch of md5", e, start);
        } catch (IOException e) {
            return Result.abort("Unrecoverable error", e, start);
        }

        return Result.correct(start);
    }

}
