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
package uk.ac.ebi.ega.cmdline.fire.archiver.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StableIdGeneratorTest {

    private static final String PREFIX = "CMD_";

    @Test
    public void generatedStableIdShouldNotBeBlank() {
        final String generatedStableId = new StableIdGenerator(PREFIX).generate();

        assertThat(generatedStableId).isNotBlank();
        assertThat(generatedStableId).startsWith(PREFIX);
        assertThat(generatedStableId.length()).isGreaterThan(PREFIX.length());
    }

}