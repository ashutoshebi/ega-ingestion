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
package uk.ac.ebi.ega.encryption.core.stream;

import java.io.IOException;
import java.io.OutputStream;

public class SingleStreamSink implements StreamSink {

    private final OutputStream sink;

    public SingleStreamSink(OutputStream sink) {
        this.sink = sink;
    }

    @Override
    public void close() throws IOException {
        sink.flush();
        sink.close();
    }

    @Override
    public void write(byte[] buffer, int i, int bytesRead) throws IOException {
        sink.write(buffer, i, bytesRead);
    }

    @Override
    public void flush() throws IOException {
        sink.flush();
    }

}