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
package uk.ac.ebi.ega.file.encryption.processor.model;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Result<T> {

    public enum Status {

        SUCCESS(1),
        FAILURE(2);

        private int enumValue;

        Status(final int enumValue) {
            this.enumValue = enumValue;
        }

        public static Status getStatusBy(final int enumValue) {
            for (final Status status : Status.values()) {
                if (enumValue == status.getEnumValue()) {
                    return status;
                }
            }
            throw new IllegalArgumentException("No matching Status for value " + enumValue);
        }

        public int getEnumValue() {
            return enumValue;
        }
    }

    private T data;
    private Status status;
    private String message;
    private Exception exception;

    private Result(final Status status, final String message, final Exception exception) {
        this.status = status;
        this.message = message;
        this.exception = exception;
    }

    private Result(final Status status, final T data) {
        this.status = status;
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public static <T> Result success(final T data) {
        return new Result(Status.SUCCESS, data);
    }

    public static Result failure(String msg, Exception e) {
        return new Result(Status.FAILURE, msg, e);
    }

    public String getMessageAndException() {
        if (message == null && exception == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if (message != null) {
            stringBuilder.append(message);
        }
        if (exception != null) {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            if (message != null) {
                stringBuilder.append("\n");
            }
            stringBuilder.append(stringWriter.toString());
        }
        return stringBuilder.toString();
    }

    public T getData() {
        return data;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Exception getException() {
        return exception;
    }
}
