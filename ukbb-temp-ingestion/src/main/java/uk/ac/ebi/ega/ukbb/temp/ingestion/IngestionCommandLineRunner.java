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
package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import uk.ac.ebi.ega.ukbb.temp.ingestion.exceptions.TerminateProgramException;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IUkbbReEncryptProcessService;

import java.util.Optional;

public class IngestionCommandLineRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(IngestionCommandLineRunner.class);

    private ApplicationContext applicationContext;

    private IUkbbReEncryptProcessService reEncryptProcessService;

    public IngestionCommandLineRunner(ApplicationContext applicationContext,
                                      IUkbbReEncryptProcessService reEncryptProcessService) {
        this.applicationContext = applicationContext;
        this.reEncryptProcessService = reEncryptProcessService;
    }

    @Override
    public void run(String... args) throws Exception {
        final Optional<CommandLineParser> optionalVars = CommandLineParser.parse(args);
        System.exit(SpringApplication.exit(applicationContext, () -> {
            if (optionalVars.isPresent()) {
                try {
                    final CommandLineParser vars = optionalVars.get();
                    reEncryptProcessService.reEncrypt(vars.getSrcKeyFile(), vars.getDstKeyFile(), vars.getFilePath());
                    return 0;
                } catch (TerminateProgramException e) {
                    return handleTerminateProgramException(e);
                }
            } else {
                return 1;
            }
        }));
    }

    private int handleTerminateProgramException(TerminateProgramException e) {
        if (e.getTermCode() == 0) {
            logger.info(e.getMessage());
        } else {
            if (e.getCause() != null) {
                logger.error(e.getMessage(), e.getCause());
            } else {
                logger.error(e.getMessage());
            }
        }
        return e.getTermCode();
    }

}
