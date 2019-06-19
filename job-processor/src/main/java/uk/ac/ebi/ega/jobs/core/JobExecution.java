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

import java.util.Objects;

public class JobExecution<T extends JobParameters> {

    private String jobId;

    private String jobName;

    private final T jobParameters;

    public JobExecution(String jobId, String jobName, T jobParameters) {
        this.jobId = Objects.requireNonNull(jobId);
        this.jobName = Objects.requireNonNull(jobName);
        this.jobParameters = Objects.requireNonNull(jobParameters);
    }

    public String getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public T getJobParameters() {
        return jobParameters;
    }

}
