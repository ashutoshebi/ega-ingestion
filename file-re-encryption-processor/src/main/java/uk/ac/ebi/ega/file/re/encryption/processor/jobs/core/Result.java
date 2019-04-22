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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

public class Result {

    public enum Status {

        SUCCESS,

        FAILURE,

        ABORTED;

    }

    private Status status;

    private String message;

    private Exception exception;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public Result(Status status, String message, Exception exception, LocalDateTime startTime) {
        this(status, message, exception, startTime, LocalDateTime.now());
    }

    public Result(Status status, String message, Exception exception, LocalDateTime startTime, LocalDateTime endTime) {
        this.status = status;
        this.message = message;
        this.exception = exception;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public static Result correct(LocalDateTime startTime) {
        return new Result(Status.SUCCESS, null, null, startTime);
    }

    public static Result failure(String msg, Exception e, LocalDateTime startTime) {
        return new Result(Status.FAILURE, msg, e, startTime);
    }

    public static Result abort(String msg, Exception e, LocalDateTime startTime) {
        return new Result(Status.ABORTED, msg, e, startTime);
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

}
