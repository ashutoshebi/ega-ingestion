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
package uk.ac.ebi.ega.ukbb.temp.ingestion.exceptions;

import java.io.File;
import java.nio.file.Path;

public class TerminateProgramException extends Exception {

    public int termCode;

    public TerminateProgramException(int termCode, String s) {
        super(s);
        termCode = termCode;
    }

    public TerminateProgramException(int i, String s, Exception e) {
        super(s, e);
        termCode = termCode;
    }

    public static TerminateProgramException fileNotFound(Path path) {
        return new TerminateProgramException(1, "File '" + path + "' could not be read.");
    }

    public static TerminateProgramException fileNotInDataset(File file) {
        return new TerminateProgramException(2, "File '" + file.getAbsolutePath() + "' does not exist in dataset.");
    }

    public static TerminateProgramException unexpectedException(Exception e) {
        return new TerminateProgramException(0, "Unexpected exception.", e);
    }

    public static TerminateProgramException checksumMissmatch() {
        return new TerminateProgramException(3, "Checksum missmatch");
    }

    public int getTermCode() {
        return termCode;
    }
}
