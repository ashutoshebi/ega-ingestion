/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ega.encryption.core.utils.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;

public class ReportingOutputStream extends OutputStream {

    private static final int DEFAULT_DELTA_TIME_MILLISECONDS = 60000;

    private final static Logger logger = LoggerFactory.getLogger(ReportingOutputStream.class);

    private final OutputStream outputStream;

    private final long deltaTimeMilliseconds;

    private long totalBytes;

    private long lastReportBytes;

    private Instant lastReport;

    public ReportingOutputStream(OutputStream outputStream) {
        this(outputStream, DEFAULT_DELTA_TIME_MILLISECONDS);
    }

    public ReportingOutputStream(OutputStream outputStream, long deltaTimeMilliseconds) {
        this.outputStream = outputStream;
        this.deltaTimeMilliseconds = deltaTimeMilliseconds;
        totalBytes = 0;
        lastReportBytes = 0;
        lastReport = Instant.now();
    }

    @Override
    public void write(int i) throws IOException {
        outputStream.write(i);
        doReport(1);
    }

    private void doReport(int i) {
        totalBytes += i;
        Instant now = Instant.now();
        long elapsedMilliseconds = Duration.between(lastReport, now).toMillis();
        if (elapsedMilliseconds >= deltaTimeMilliseconds) {
            logReport(elapsedMilliseconds);
            lastReport = now;
            lastReportBytes = totalBytes;
        }
    }

    private void logReport(long elapsedMilliseconds) {
        String total = String.format("%.02f", convertToUnit(totalBytes));
        String speed = String.format("%.02f", calculateSpeed(totalBytes, lastReportBytes, elapsedMilliseconds));
        logger.info("Total written {} Gb at a rate of {} Mb/s", total, speed);
    }

    private double calculateSpeed(long totalBytes, long lastReportBytes, long elapsedMillisecons) {
        return ((double) (totalBytes - lastReportBytes) / elapsedMillisecons) * ((double) 1000L / 1048576L);
    }

    private static double convertToUnit(long bytes) {
        return (double) bytes / 1073741824L;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
        doReport(bytes.length);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        outputStream.write(bytes, offset, length);
        doReport(length);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
