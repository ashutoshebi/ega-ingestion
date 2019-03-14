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

import uk.ac.ebi.ega.encryption.core.stream.MultipleStreamSink;
import uk.ac.ebi.ega.encryption.core.stream.SingleStreamSink;
import uk.ac.ebi.ega.encryption.core.stream.StreamSource;

import java.io.InputStream;
import java.io.OutputStream;

public class StreamPipelineBuilder {

    public static class StreamSourceBuilder {

        private final InputStream source;

        private int bufferSize = 8192;

        public StreamSourceBuilder(InputStream source) {
            this.source = source;
        }

        public StreamSourceBuilder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public StreamSource to(OutputStream... sinks) {
            if (sinks.length == 1) {
                return new StreamSource(source, bufferSize, new SingleStreamSink(sinks[0]));
            } else {
                return new StreamSource(source, bufferSize, new MultipleStreamSink(sinks));
            }
        }

    }

    public static StreamSourceBuilder source(InputStream source) {
        return new StreamSourceBuilder(source);
    }

}
