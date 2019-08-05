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
package uk.ac.ebi.ega.cmdline.fire.archiver;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

class CommandLineParser {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineParser.class);

    private static final String FILE_PATH = "filePath";

    private static final String PATH_ON_FIRE = "pathOnFire";

    private final Path filePath;

    private final String pathOnFire;

    private CommandLineParser(OptionSet optionSet) {
        filePath = Paths.get((String) optionSet.valueOf(FILE_PATH));
        pathOnFire = optionSet.valueOf(PATH_ON_FIRE).toString();
    }

    static Optional<CommandLineParser> parse(String... parameters) throws IOException {
        OptionParser parser = buildParser();
        try {
            return Optional.of(new CommandLineParser(parser.parse(parameters)));
        } catch (OptionException e) {
            logger.error("Invalid command-line arguments were received:");
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
        parser.accepts(PATH_ON_FIRE, "Path of the output file on Fire").withRequiredArg().required().ofType(String.class);
        parser.allowsUnrecognizedOptions();
        return parser;
    }

    Path getFilePath() {
        return filePath;
    }

    String getPathOnFire() {
        return pathOnFire;
    }
}
