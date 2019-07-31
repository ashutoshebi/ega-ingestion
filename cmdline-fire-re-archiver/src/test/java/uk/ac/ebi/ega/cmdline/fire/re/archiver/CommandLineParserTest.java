/*
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
 */
package uk.ac.ebi.ega.cmdline.fire.re.archiver;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CommandLineParserTest {

    @Test
    public void testParseEmptyParameters() throws IOException {
        assertFalse(CommandLineParser.parse(new String[]{}).isPresent());
    }

    @Test
    public void testMissingMandatoryParameter() throws IOException {
        assertFalse(CommandLineParser.parse(new String[]{"--filePath=/file2"}).isPresent());
    }

    @Test
    public void testMandatoryParameter() throws IOException {
        final CommandLineParser parser = CommandLineParser.parse(new String[]{
                "--pathOnFire=/file",
                "--filePath=/file2"})
                .orElseThrow(AssertionError::new);
        assertEquals("/file", parser.getPathOnFire());
        assertEquals("/file2", parser.getFilePath().toString());
    }

    @Test
    public void testOptionalParameter() throws IOException {
        final CommandLineParser parser = CommandLineParser.parse(new String[]{
                "--pathOnFire=/file",
                "--filePath=/file2",
                "--optionalParameter=/file3"})
                .orElseThrow(AssertionError::new);
        assertEquals("/file", parser.getPathOnFire());
        assertEquals("/file2", parser.getFilePath().toString());
    }

}
