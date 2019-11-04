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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.ReEncryptJob;
import uk.ac.ebi.ega.jobs.core.Job;
import uk.ac.ebi.ega.jobs.core.JobExecution;
import uk.ac.ebi.ega.jobs.core.Result;
import uk.ac.ebi.ega.jobs.core.exceptions.JobNotRegistered;
import uk.ac.ebi.ega.jobs.core.persistence.entity.JobExecutionEntity;
import uk.ac.ebi.ega.jobs.core.persistence.entity.JobRun;
import uk.ac.ebi.ega.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration.DelayType;
import uk.ac.ebi.ega.file.re.encryption.processor.messages.ReEncryptComplete;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.entity.ReEncryptParametersEntity;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.ReEncryptParametersRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.service.ReEncryptJobParameterService;
import uk.ac.ebi.ega.fire.LocalStorageFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReEncryptServiceTest {

    private static final String EMPTY_STRING = "";
    private static final String BASE_PATH = "target/test-classes/keyPairTest/";
    private static final String JOB_NAME = "re-encrypt-job";
    private static final String RE_ENCRYPT_FILE_NAME = "test_file_reencrypt.txt.gpg";
    private static final String PASSWORD = "test";
    private static final char[] PASSWORD_KEY = "test_password".toCharArray();

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private JobRunRepository jobRunRepository;

    @Mock
    private ReEncryptParametersRepository reEncryptParametersRepository;

    private ReEncryptService reEncryptService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void init() {
        final IPasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService(PASSWORD_KEY);

        final ReEncryptJobParameterService reEncryptJobParameterService = new ReEncryptJobParameterService(
                reEncryptParametersRepository, passwordEncryptionService);
        MockitoAnnotations.initMocks(reEncryptJobParameterService);

        final IMailingService mailingService = Mockito.mock(IMailingService.class);
        final Job<ReEncryptJobParameters> job = new ReEncryptJob(dosId -> new LocalStorageFile(
                "b2e6283b2044de260d6df0e854cd3fa2", BASE_PATH + "test_file.txt.gpg"), EMPTY_STRING.toCharArray());

        final KafkaTemplate<String, ReEncryptComplete> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        final DelayConfiguration delayConfiguration = new DelayConfiguration(DelayType.LINEAR, 0, 0);
        final ReEncryptPersistenceService reEncryptPersistenceService = new ReEncryptPersistenceService(jobExecutionRepository, jobRunRepository, EMPTY_STRING, reEncryptJobParameterService);
        reEncryptService = new ReEncryptService(reEncryptPersistenceService, passwordEncryptionService, mailingService,
                EMPTY_STRING, job, kafkaTemplate, EMPTY_STRING, delayConfiguration);
        MockitoAnnotations.initMocks(reEncryptService);
    }

    @Test
    public void createJob_SuppliedCorrectArguments_ExecutesSuccessfully() throws JobNotRegistered {

        when(jobExecutionRepository.save(any(JobExecutionEntity.class))).thenReturn(new JobExecutionEntity());

        final Optional<JobExecution<ReEncryptJobParameters>> jobExecutionOptional = reEncryptService.createJob(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);

        assertTrue(jobExecutionOptional.isPresent());
    }

    @Test
    public void reEncrypt_SuppliedCorrectArgument_ExecutesSuccessfully() throws AlgorithmInitializationException,
            IOException {
        final IPasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService(PASSWORD_KEY);
        String encryptedPassword = passwordEncryptionService.encrypt(IOUtils.convertToBytes(PASSWORD.toCharArray()));

        final ReEncryptJobParameters reEncryptJobParameters = new ReEncryptJobParameters(passwordEncryptionService,
                EMPTY_STRING, temporaryFolder.newFile(RE_ENCRYPT_FILE_NAME).getAbsolutePath(),
                encryptedPassword);
        final JobExecution<ReEncryptJobParameters> jobExecution = new JobExecution<>(EMPTY_STRING, JOB_NAME, reEncryptJobParameters);

        when(jobRunRepository.save(any(JobRun.class))).thenReturn(new JobRun());

        final Result result = reEncryptService.reEncrypt(jobExecution);

        assertEquals(Result.Status.SUCCESS, result.getStatus());
    }

    @Test
    public void getUnfinishedJob_ExecutesSuccessfullyAndReturnsUnfinishedJob() {

        final ReEncryptParametersEntity reEncryptParametersEntity = new ReEncryptParametersEntity(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);

        when(jobExecutionRepository.findByInstanceId(anyString())).thenReturn(Optional.of(new JobExecutionEntity(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING)));
        when(reEncryptParametersRepository.findById(anyString())).thenReturn(Optional.of(reEncryptParametersEntity));

        final Optional<JobExecution<ReEncryptJobParameters>> jobExecutionOptional = reEncryptService.getUnfinishedJob();

        assertTrue(jobExecutionOptional.isPresent());
    }
}
