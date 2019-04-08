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
package uk.ac.ebi.ega.file.re.encryption.processor.pipelines;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CmdLineProcessPiped {

    private static final int BUFFER_SIZE = 8192;

    private Process process;

    private Future<?> pipeBufferTask;

    private ExecutorService executorService;

    private File errorRedirect;

    public CmdLineProcessPiped(File errorRedirect, String... command) throws IOException {
        this.executorService = ForkJoinPool.commonPool();
        ProcessBuilder pb = new ProcessBuilder(command);
        if (errorRedirect != null) {
            pb.redirectError(errorRedirect);
            this.errorRedirect = errorRedirect;
        }
        process = pb.start();
    }

    public synchronized void pipeOutputTo(OutputStream output) {
        if (pipeBufferTask != null && process.isAlive())
            pipeBufferTask = executorService.submit(() -> {
                final InputStream inputStream = process.getInputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                try {
                    int bytesRead = inputStream.read(buffer);
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead);
                        bytesRead = inputStream.read(buffer);
                    }
                    output.flush();
                } catch (IOException e) {
                    terminateProcess();
                }
            });
    }

    public OutputStream getPipeToProcess() {
        return process.getOutputStream();
    }

    public void terminateProcess() {
        if (pipeBufferTask != null) {
            pipeBufferTask.cancel(true);
        }
        process.destroyForcibly();
    }

    public boolean waitFor(long time, TimeUnit timeUnit) {
        try {
            return process.waitFor(time, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int exitValue() {
        return process.exitValue();
    }

    public File getErrorRedirect() {
        return errorRedirect;
    }

    public String getErrorRedirectLog() throws IOException {
        if (errorRedirect == null) {
            throw new IOException("Error output was not redirected");
        }
        return Files.readAllLines(errorRedirect.toPath()).stream().collect(Collectors.joining("\n"));
    }

}
