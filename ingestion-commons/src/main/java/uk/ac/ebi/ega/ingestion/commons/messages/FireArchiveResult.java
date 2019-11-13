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
package uk.ac.ebi.ega.ingestion.commons.messages;

public class FireArchiveResult {

    public enum Status {
        SUCCESS, FAILURE, EXISTS, RETRY;
    }

    private FireResponse responseData;
    private Status status;
    private String message;

    private FireArchiveResult() {
    }

    private FireArchiveResult(final FireResponse responseData, final Status status) {
        this.responseData = responseData;
        this.status = status;
    }

    private FireArchiveResult(final Status status, final String message) {
        this.status = status;
        this.message = message;
    }

    public FireResponse getResponseData() {
        return responseData;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public static FireArchiveResult success(final FireResponse responseData) {
        return new FireArchiveResult(responseData, Status.SUCCESS);
    }

    public static FireArchiveResult failure(final String message) {
        return new FireArchiveResult(Status.FAILURE, message);
    }

    public static FireArchiveResult exists(final String message) {
        return new FireArchiveResult(Status.EXISTS, message);
    }

    public static FireArchiveResult retry(final String message) {
        return new FireArchiveResult(Status.RETRY, message);
    }

    @Override
    public String toString() {
        return "Result{" +
                "responseData=" + responseData +
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
