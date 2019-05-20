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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.ReEncryptJob;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Job;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobExecution;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.entity.JobExecutionEntity;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.entity.JobRun;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.messages.ReEncryptComplete;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.entity.ReEncryptParametersEntity;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.ReEncryptParametersRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.service.ReEncryptJobParameterService;
import uk.ac.ebi.ega.fire.LocalStorageFile;

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReEncryptServiceTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private JobRunRepository jobRunRepository;

    @Mock
    private ReEncryptParametersRepository reEncryptParametersRepository;

    private ReEncryptService reEncryptService;

    private static final String EMPTY_STRING = "";
    private static final String BASE_PATH = "src/test/resources/keyPairTest/";
    private static final String JOB_NAME = "re-encrypt-job";
    private static final String REENCRYPT_FILE_NAME = "test_file_reencrypt.txt.gpg";
    private static final String PASSWORD = "test";

    @Before
    public void init() {

        final ReEncryptJobParameterService reEncryptJobParameterService = new ReEncryptJobParameterService(reEncryptParametersRepository);
        MockitoAnnotations.initMocks(reEncryptJobParameterService);

        final IMailingService mailingService = Mockito.mock(IMailingService.class);
        final Job<ReEncryptJobParameters> job = new ReEncryptJob(dosId -> new LocalStorageFile("b2e6283b2044de260d6df0e854cd3fa2", BASE_PATH + "test_file.txt.gpg"), PASSWORD.toCharArray());

        final KafkaTemplate<String, ReEncryptComplete> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        final ReEncryptPersistenceService reEncryptPersistenceService = new ReEncryptPersistenceService(jobExecutionRepository, jobRunRepository, EMPTY_STRING, reEncryptJobParameterService);
        reEncryptService = new ReEncryptService(reEncryptPersistenceService, mailingService, EMPTY_STRING, job, kafkaTemplate, EMPTY_STRING);
        MockitoAnnotations.initMocks(reEncryptService);
    }

    @Test
    public void createJob_SuppliedCorrectArguments_ExecutesSuccessfully() {

        when(jobExecutionRepository.save(any(JobExecutionEntity.class))).thenReturn(new JobExecutionEntity());

        final Optional<JobExecution<ReEncryptJobParameters>> jobExecutionOptional = reEncryptService.createJob(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, new char[0]);

        assertTrue(jobExecutionOptional.isPresent());
    }

    @Test
    public void reEncrypt_SuppliedCorrectArgument_ExecutesSuccessfully() {

        final ReEncryptJobParameters reEncryptJobParameters = new ReEncryptJobParameters(EMPTY_STRING, new File(BASE_PATH + REENCRYPT_FILE_NAME).getAbsolutePath(), PASSWORD.toCharArray());
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