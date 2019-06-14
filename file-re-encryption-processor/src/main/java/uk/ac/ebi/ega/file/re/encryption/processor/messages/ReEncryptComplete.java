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
package uk.ac.ebi.ega.file.re.encryption.processor.messages;

import uk.ac.ebi.ega.jobs.core.Result;

import java.time.LocalDateTime;

public class ReEncryptComplete {

    private String status;

    private String message;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public ReEncryptComplete(Result.Status status, String message, LocalDateTime startTime, LocalDateTime endTime) {
        this.status = status.toString();
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

}
