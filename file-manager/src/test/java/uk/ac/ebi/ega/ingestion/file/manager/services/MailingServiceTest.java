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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxAssignation;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class MailingServiceTest {

    public static final char[] CHAR_ARRAY = "kiwi".toCharArray();

    private static final String ANY_STRING = "any";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private IPasswordEncryptionService passwordEncryptionService;

    @InjectMocks
    private MailingService mailingService;

    /**
     * To test sendDownloadBoxFinishedMail execute successfully when DownloadBox given.
     */
    @Test
    public void sendDownloadBoxFinishedMail_DownloadBoxGiven_ShouldExecutesSuccessfully()
            throws AlgorithmInitializationException {

        final DownloadBoxJob downloadBoxJob = new DownloadBoxJob();
        downloadBoxJob.setAssignedDownloadBox(new DownloadBoxAssignation());
        downloadBoxJob.setPassword(ANY_STRING);

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        when(passwordEncryptionService.decrypt(any(String.class))).thenReturn(CHAR_ARRAY);

        mailingService.sendDownloadBoxFinishedMail(downloadBoxJob);
    }
}
