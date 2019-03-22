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
import java.io.InputStream;
import java.io.OutputStream;

public class SimpleStream implements PipelineStream {

    private final InputStream source;

    private final int bufferSize;

    private long totalRead;

    private final OutputStream outputStream;

    public SimpleStream(InputStream source, int bufferSize, OutputStream outputStream) {
        this.source = source;
        this.bufferSize = bufferSize;
        this.totalRead = 0;
        this.outputStream = outputStream;
    }

    @Override
    public void close() throws IOException {
        source.close();
        outputStream.close();
    }

    @Override
    public void execute() throws IOException {
        byte[] buffer = new byte[bufferSize];
        int bytesRead = source.read(buffer);
        while (bytesRead != -1) {
            totalRead += bytesRead;
            outputStream.write(buffer, 0, bytesRead);
            bytesRead = source.read(buffer);
        }
        outputStream.flush();
    }

    public InputStream getSource() {
        return source;
    }

}