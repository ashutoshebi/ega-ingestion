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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CommandLineParser {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineParser.class);

    private static final String FILE_PATH = "filePath";

    private static final String SOURCE_PASSWORD_FILE = "srcKeyFile";

    private static final String DESTINATION_PASSWORD_FILE = "dstKeyFile";

    private static final String DISABLE_INGESTION = "disableIngestion";

    private final Path filePath;

    private final Path srcKeyFile;

    private final Path dstKeyFile;

    private final boolean disableIngestion;

    private CommandLineParser(OptionSet optionSet) {
        filePath = Paths.get((String) optionSet.valueOf(FILE_PATH));
        srcKeyFile = Paths.get((String) optionSet.valueOf(SOURCE_PASSWORD_FILE));
        dstKeyFile = Paths.get((String) optionSet.valueOf(DESTINATION_PASSWORD_FILE));
        disableIngestion = optionSet.has(DISABLE_INGESTION);
    }

    public static Optional<CommandLineParser> parse(String... parameters) throws IOException {
        OptionParser parser = buildParser();
        try {
            return Optional.of(new CommandLineParser(parser.parse(parameters)));
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            return Optional.empty();
        } catch (RuntimeException e) {
            logger.error(e.getMessage(),e);
            return Optional.empty();
        }
    }

    private static OptionParser buildParser() {
        OptionParser parser = new OptionParser();
        parser.accepts(FILE_PATH, "File source path").withRequiredArg().required().ofType(String.class);
        parser.accepts(SOURCE_PASSWORD_FILE, "Path of password file to read source file")
                .withRequiredArg().required().ofType(String.class);
        parser.accepts(DESTINATION_PASSWORD_FILE, "Path of password file to write destination file")
                .withRequiredArg().required().ofType(String.class);
        parser.accepts(DISABLE_INGESTION, "Disable file ingestion into FIRE");
        parser.allowsUnrecognizedOptions();
        return parser;
    }

    public Path getFilePath() {
        return filePath;
    }

    public Path getSrcKeyFile() {
        return srcKeyFile;
    }

    public Path getDstKeyFile() {
        return dstKeyFile;
    }

    public boolean isDisableIngestion() {
        return disableIngestion;
    }

}
