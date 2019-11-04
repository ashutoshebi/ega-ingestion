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

import java.io.PrintWriter;
import java.io.StringWriter;

public class FileEncryptionResult {

    public enum Status {

        SUCCESS(1),

        MD5_ERROR(2),

        FAILURE(3);

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

    private Status status;

    private String message;

    private FileEncryptionData data;

    FileEncryptionResult() {
    }

    private FileEncryptionResult(final Status status, final String message, final Exception exception) {
        this.status = status;
        this.message = formatMessageAndException(message, exception);
    }

    private FileEncryptionResult(final Status status, final FileEncryptionData data) {
        this.status = status;
        this.data = data;
    }

    public static FileEncryptionResult success(final FileEncryptionData data) {
        return new FileEncryptionResult(Status.SUCCESS, data);
    }

    public static FileEncryptionResult failure(String msg, Exception e) {
        return new FileEncryptionResult(Status.FAILURE, msg, e);
    }

    public static FileEncryptionResult md5Failure(String msg, Exception e) {
        return new FileEncryptionResult(Status.MD5_ERROR, msg, e);
    }

    private static String formatMessageAndException(String message, Exception exception) {
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

    public FileEncryptionData getData() {
        return data;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}
