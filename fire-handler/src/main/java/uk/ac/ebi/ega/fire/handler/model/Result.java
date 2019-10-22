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
package uk.ac.ebi.ega.fire.handler.model;

/**
 * Not making responseData generic as it doesn't necessary.
 * If required in future can be done.
 */
public class Result {

    public enum Status {
        SUCCESS, FAILURE, EXISTS, RETRY;
    }

    private FireResponse responseData;
    private Status status;
    private String message;

    private Result() {
    }

    private Result(final FireResponse responseData, final Status status) {
        this.responseData = responseData;
        this.status = status;
    }

    private Result(final Status status, final String message) {
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

    public static Result success(final FireResponse responseData) {
        return new Result(responseData, Status.SUCCESS);
    }

    public static Result failure(final String message) {
        return new Result(Status.FAILURE, message);
    }

    public static Result exists(final String message) {
        return new Result(Status.EXISTS, message);
    }

    public static Result retry(final String message) {
        return new Result(Status.RETRY, message);
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
