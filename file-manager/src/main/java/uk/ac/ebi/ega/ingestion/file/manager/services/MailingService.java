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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MailingService implements IMailingService {

    private Logger logger = LoggerFactory.getLogger(MailingService.class);

    private JavaMailSender mailSender;

    private IPasswordEncryptionService passwordEncryptionService;

    private String errorMail;

    public MailingService(JavaMailSender mailSender, IPasswordEncryptionService passwordEncryptionService,
                          String errorMail) {
        this.mailSender = mailSender;
        this.passwordEncryptionService = passwordEncryptionService;
        this.errorMail = errorMail;
    }

    @Override
    public void sendDownloadBoxFinishedMail(DownloadBoxJob job) {
        char[] password = null;
        try {
            password = passwordEncryptionService.decrypt(job.getPassword());
        } catch (AlgorithmInitializationException e) {
            //TODO this should never happen
            logger.error(e.getMessage(), e);
            reportError("Error while decrypting database password", e);
        }

        String subject = String.format(
                "Dataset '%s' is ready to download",
                job.getDatasetId()
        );
        String text = String.format(
                "Your request with id '%s' for Dataset '%s' has been processed.\n" +
                        "You can download it from '%s'\n" +
                        "Your private password to decrypt the information is '%s'\n\n" +
                        "Attention: This download box will expire on %s\n\n" +
                        "For further assistance, please include the request id when contacting EGA HelpDesk.",
                job.getTicketId(),
                job.getDatasetId(),
                job.getAssignedDownloadBox().getBoxId(),
                new String(password),
                job.getAssignedDownloadBox().getUntilDate()
        );
        sendSimpleMessage(job.getMail(), subject, text);
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    @Override
    public void reportError(String subject, Exception e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        sendSimpleMessage(errorMail, subject, stringWriter.toString());
    }

}
