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
package uk.ac.ebi.ega.encryption.core;

import uk.ac.ebi.ega.encryption.core.stream.ParallelSplitStream;
import uk.ac.ebi.ega.encryption.core.stream.PipelineStream;
import uk.ac.ebi.ega.encryption.core.stream.SimpleStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class StreamPipelineBuilder {

    public static class StreamSourceBuilder {

        private final InputStream source;

        private int bufferSize = 8192;

        private final List<OutputStream> outputoutputStreams;

        public StreamSourceBuilder(InputStream source) {
            this.source = source;
            outputoutputStreams = new ArrayList<>();
        }

        public StreamSourceBuilder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public StreamSourceBuilder to(OutputStream outputStream) {
            outputoutputStreams.add(outputStream);
            return this;
        }

        public PipelineStream build() {
            if (outputoutputStreams.size() == 1) {
                return new SimpleStream(source, bufferSize, outputoutputStreams.get(0));
            } else {
                return new ParallelSplitStream(source, bufferSize, outputoutputStreams);
            }
        }

    }

    public static StreamSourceBuilder source(InputStream source) {
        return new StreamSourceBuilder(source);
    }

}
