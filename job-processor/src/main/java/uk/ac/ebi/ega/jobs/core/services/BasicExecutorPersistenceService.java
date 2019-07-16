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
package uk.ac.ebi.ega.jobs.core.services;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.jobs.core.JobExecution;
import uk.ac.ebi.ega.jobs.core.Result;
import uk.ac.ebi.ega.jobs.core.persistence.entity.JobExecutionEntity;
import uk.ac.ebi.ega.jobs.core.persistence.entity.JobRun;
import uk.ac.ebi.ega.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.jobs.core.persistence.repository.JobRunRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BasicExecutorPersistenceService implements ExecutorPersistenceService {

    private final JobExecutionRepository jobExecutionRepository;

    private final JobRunRepository jobRunRepository;

    private String instanceId;

    private final Map<JobDefinition, JobParameterService> parameterServices;

    public BasicExecutorPersistenceService(JobExecutionRepository jobExecutionRepository,
                                           JobRunRepository jobRunRepository,
                                           String instanceId) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobRunRepository = jobRunRepository;
        this.instanceId = instanceId;
        this.parameterServices = new ConcurrentHashMap<>();
    }

    public <T> void registerJobParameterServices(String jobName, Class<T> parameterClass,
                                                 JobParameterService<T> parameterService) {
        parameterServices.put(new JobDefinition(jobName, parameterClass), parameterService);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public <T> void assignExecution(String jobId, String jobName, T jobParameters) {
        jobExecutionRepository.save(new JobExecutionEntity(jobId, jobName, instanceId));
        getJobParameterService(jobName, (Class<T>) jobParameters.getClass()).persist(jobId, jobParameters);
    }

    private <T> JobParameterService<T> getJobParameterService(String jobName,
                                                              Class<T> parameterClass) {
        return (JobParameterService<T>) parameterServices.get(new JobDefinition(jobName, parameterClass));
    }

    @Override
    @Transactional
    public void saveResult(String jobId, Result execute) {
        jobRunRepository.save(new JobRun(jobId, instanceId, execute.getMessageAndException(), execute.getStartTime(),
                execute.getEndTime()));
        if (execute.getStatus() != Result.Status.ABORTED) {
            jobExecutionRepository.deleteById(jobId);
        }
    }

    @Override
    public <T> Optional<JobExecution<T>> getAssignedExecution(String jobName,
                                                              Class<T> parameterClass) {
        final Optional<JobExecutionEntity> optional = jobExecutionRepository.findByInstanceId(instanceId);
        if (optional.isPresent()) {
            final JobExecutionEntity jobExecutionEntity = optional.get();
            final JobParameterService<T> jobParameterService = getJobParameterService(jobName, parameterClass);
            Optional<T> optionalJobParameters = jobParameterService.getParameters(jobExecutionEntity.getId());
            if (optionalJobParameters.isPresent()) {
                return Optional.of(new JobExecution<>(jobExecutionEntity.getId(), jobName,
                        optionalJobParameters.get()));
            }
        }
        return Optional.empty();
    }

}
