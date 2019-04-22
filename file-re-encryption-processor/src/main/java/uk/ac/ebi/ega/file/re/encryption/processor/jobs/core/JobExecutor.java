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
package uk.ac.ebi.ega.file.re.encryption.processor.jobs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.exceptions.JobNotRegistered;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.exceptions.JobRetryException;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.JobDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JobExecutor {

    private final Logger logger = LoggerFactory.getLogger(JobExecutor.class);

    private Map<JobDefinition, Job> jobMap;

    private ExecutorPersistenceService persistenceService;

    public JobExecutor(ExecutorPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        this.jobMap = new HashMap<>();
    }

    public <T extends JobParameters> void registerJob(String jobName, Class<T> parameterClass, Job<T> job) {
        jobMap.put(new JobDefinition(jobName, parameterClass), job);
    }

    public <T extends JobParameters> Optional<JobExecution<T>> assignExecution(String jobId, String jobName,
                                                                               T jobParameters) {
        // TODO improve error checking maybe add a method to ask if the job exists in the register with the type and
        //  return the optional empty in that case?
        try {
            persistenceService.assignExecution(jobId, jobName, jobParameters);
            return Optional.of(new JobExecution<>(jobId, jobName, jobParameters));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    public <T extends JobParameters> Optional<JobExecution<T>> getAssignedExecution(String jobName,
                                                                                    Class<T> parameterClass) {
        return persistenceService.getAssignedExecution(jobName, parameterClass);
    }

    public <T extends JobParameters> Result execute(JobExecution<T> jobExecution) throws JobNotRegistered {
        // TODO improve error checking
        final Job<T> job = getJob(jobExecution.getJobName(), (Class<T>) jobExecution.getJobParameters().getClass());
        logger.info("Executing job {}", jobExecution.getJobId());
        Result result = null;
        while (result == null) {
            try {
                result = doExecute(job, jobExecution);
            } catch (JobRetryException e) {
                logger.error("JobParameters could not be completed {}", e.getMessage());
                logger.info("Retrying job {}", jobExecution.getJobId());
                delay();
            }
        }
        return result;
    }

    private <T extends JobParameters> Job<T> getJob(String jobName, Class<T> parameterClass) {
        return (Job<T>) jobMap.get(new JobDefinition(jobName, parameterClass));
    }

    private <T extends JobParameters> Result doExecute(Job<T> job, JobExecution<T> jobExecution) {
        final Result result = job.execute(jobExecution.getJobParameters());
        persistenceService.saveResult(jobExecution.getJobId(), result);
        return result;
    }

    private void delay() {
        //TODO DELAY add a configurable backoff or linear delay
    }

}
