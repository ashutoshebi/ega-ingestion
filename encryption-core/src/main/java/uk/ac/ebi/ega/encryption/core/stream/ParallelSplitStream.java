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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class ParallelSplitStream implements PipelineStream {

    private final InputStream source;

    private final int bufferSize;

    private long totalRead;

    private final List<OutputStream> outputStreams;

    public ParallelSplitStream(InputStream source, int bufferSize, List<OutputStream> outputStreams) {
        this.source = source;
        this.bufferSize = bufferSize;
        this.totalRead = 0;
        this.outputStreams = outputStreams;
    }

    @Override
    public void close() throws IOException {
        for (OutputStream outputStream : outputStreams) {
            outputStream.flush();
            outputStream.close();
        }
    }

    @Override
    public void execute() throws IOException {
        byte[] buffer = new byte[bufferSize];
        int bytesRead = source.read(buffer);
        while (bytesRead != -1) {
            totalRead += bytesRead;
            write(buffer, 0, bytesRead);
            bytesRead = source.read(buffer);
        }
        flush();
    }

    public void write(byte[] buffer, int i, int bytesRead) throws IOException {
        final List<CompletableFuture<Void>> futures = outputStreams.stream().map(outputStream -> doWrite(outputStream,
                buffer, i, bytesRead)).collect(Collectors.toList());

        final CompletableFuture[] completableFutures = futures.toArray(new CompletableFuture[futures.size()]);
        try {
            CompletableFuture.allOf(completableFutures).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    public CompletableFuture<Void> doWrite(OutputStream outputStream, byte[] buffer, int i, int bytesRead) {
        return CompletableFuture.runAsync(() -> {
            try {
                outputStream.write(buffer, i, bytesRead);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public void flush() throws IOException {
        for (OutputStream outputStream : outputStreams) {
            outputStream.flush();
        }
    }

}
