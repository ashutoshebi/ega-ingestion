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
package uk.ac.ebi.ega.file.re.encryption.processor.models;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ReEncryptResult {

    public enum Status {

        CORRECT,

        ERROR,

        RETRY

    }

    private Status status;

    private String message;

    public ReEncryptResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public ReEncryptResult(String message, Exception exception) {
        this.status = Status.ERROR;
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        this.message = message + "\nTrace:\n" + stringWriter.toString();
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}
