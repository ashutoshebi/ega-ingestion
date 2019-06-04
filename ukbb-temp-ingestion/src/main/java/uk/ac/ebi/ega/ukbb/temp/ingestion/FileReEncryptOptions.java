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
package uk.ac.ebi.ega.ukbb.temp.ingestion;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class FileReEncryptOptions {

    private static final Logger logger = LoggerFactory.getLogger(FileReEncryptOptions.class);

    private static final String EGA_ID = "egaId";
    private static final String EGAD = "EGAD";
    private static final String EGAF = "EGAF";

    private String egaId;

    private boolean dataset;

    private FileReEncryptOptions(OptionSet optionSet) {
        egaId = (String) optionSet.valueOf(EGA_ID);
        if (egaId.startsWith(EGAD)) {
            dataset = true;
        } else {
            if (!egaId.startsWith(EGAF)) {
                throw new RuntimeException();
            }
        }
    }

    public static Optional<FileReEncryptOptions> parse(String... parameters) throws IOException {
        OptionParser parser = buildParser();
        try {
            return Optional.of(new FileReEncryptOptions(parser.parse(parameters)));
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            return Optional.empty();
        } catch (RuntimeException e) {
            logger.error("Id must be EGAD or EGAF");
            return Optional.empty();
        }
    }

    private static OptionParser buildParser() {
        OptionParser parser = new OptionParser();
        parser.accepts(EGA_ID, "EGA id").withRequiredArg().required().ofType(String.class);
        parser.allowsUnrecognizedOptions();
        return parser;
    }

    public String getEgaId() {
        return egaId;
    }

    public boolean isDataset() {
        return dataset;
    }

}
