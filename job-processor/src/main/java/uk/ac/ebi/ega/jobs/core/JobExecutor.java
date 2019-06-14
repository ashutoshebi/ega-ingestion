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
package uk.ac.ebi.ega.jobs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import uk.ac.ebi.ega.jobs.core.exceptions.JobNotRegistered;
import uk.ac.ebi.ega.jobs.core.exceptions.JobRetryException;
import uk.ac.ebi.ega.jobs.core.services.JobDefinition;
import uk.ac.ebi.ega.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration;
import uk.ac.ebi.ega.jobs.core.utils.Delayer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JobExecutor {

    private final Logger logger = LoggerFactory.getLogger(JobExecutor.class);

    private final Map<JobDefinition, Job> jobMap;

    private final ExecutorPersistenceService persistenceService;

    private DelayConfiguration delayConfiguration;

    public JobExecutor(ExecutorPersistenceService persistenceService,
                       final DelayConfiguration delayConfiguration) {
        this.persistenceService = persistenceService;
        this.delayConfiguration = delayConfiguration;
        this.jobMap = new ConcurrentHashMap<>();
    }

    public <T extends JobParameters> void registerJob(String jobName, Class<T> parameterClass, Job<T> job) {
        jobMap.put(new JobDefinition(jobName, parameterClass), job);
    }

    public <T extends JobParameters> Optional<JobExecution<T>> assignExecution(String jobId, String jobName,
                                                                               T jobParameters) throws JobNotRegistered {
        final Class<? extends JobParameters> parameterClass = jobParameters.getClass();

        if (isJobNotYetRegistered(jobName, parameterClass)) {
            logger.error("Fatal error: Job with name {} and parameterClass {} is not yet registered.",
                    jobName, parameterClass);
            throw new JobNotRegistered(jobId);
        }

        if (isExecutionAlreadyAssigned(jobName, jobParameters)) {
            logger.debug("JobExecution with name {} and parameterClass {} is already assigned, " +
                    "skipping execution assignment.", jobName, parameterClass);
            return Optional.empty();
        }

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
        final Job<T> job = getJob(jobExecution.getJobName(), (Class<T>) jobExecution.getJobParameters().getClass());

        if (job == null) {
            throw new JobNotRegistered(jobExecution.getJobId());
        }

        logger.info("Executing job {}", jobExecution.getJobId());
        Result result = null;
        final Delayer delayer = Delayer.create(delayConfiguration);

        while (result == null) {
            try {
                result = doExecute(job, jobExecution);
            } catch (JobRetryException e) {
                logger.error("JobParameters could not be completed {}", e.getMessage());
                logger.info("Retrying job {}", jobExecution.getJobId());
                delayer.delay();
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

    private <T extends JobParameters> boolean isJobNotYetRegistered(final String jobName, final Class<T> parameterClass) {
        final Job<T> job = getJob(jobName, parameterClass);
        return job == null;
    }

    private <T extends JobParameters> boolean isExecutionAlreadyAssigned(final String jobName, final T jobParameters) {
        return getAssignedExecution(jobName, jobParameters.getClass()).isPresent();
    }

}
