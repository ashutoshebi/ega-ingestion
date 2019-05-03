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

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MailingService implements IMailingService {

    private JavaMailSender mailSender;

    private String applicationName;

    private String instanceId;

    public MailingService(JavaMailSender mailSender, String applicationName, String instanceId) {
        this.mailSender = mailSender;
        this.applicationName = applicationName;
        this.instanceId = instanceId;
    }

    @Override
    public void sendSimpleMessage(String to, String subject, Exception e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        sendSimpleMessage(to, subject, stringWriter.toString());
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[" + applicationName + "-" + instanceId + "] " + subject);
        message.setText(text);
        mailSender.send(message);
    }

}
