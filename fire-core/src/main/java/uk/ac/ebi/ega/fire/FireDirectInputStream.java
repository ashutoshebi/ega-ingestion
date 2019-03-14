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
package uk.ac.ebi.ega.fire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.fire.exceptions.MaxRetryOnConnectionReached;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class FireDirectInputStream extends InputStream {

    private static final Logger logger = LoggerFactory.getLogger(FireDirectInputStream.class);

    private static final int MAX_TRIES = 10;

    private final byte[] buffer;

    private int bufferPointer;

    private int maxBufferPointer;

    private final URL url;

    private long totalRead;

    private long totalFileSize;

    private boolean allowedByteRangeRequests;

    private HttpURLConnection connection;

    private InputStream httpInputStream;

    public FireDirectInputStream(String url) throws IOException {
        this(url, url);
    }

    public FireDirectInputStream(String url, String headUrl) throws IOException {
        buffer = new byte[8192];
        bufferPointer = 0;
        maxBufferPointer = 0;
        this.url = new URL(url);
        this.totalRead = 0;
        totalFileSize = -1;
        allowedByteRangeRequests = false;
        doReadHeadOfLink(headUrl);
        logger.info("File found in file direct {} with size {}", url, totalFileSize);
    }

    private void doReadHeadOfLink(String headUrl) throws IOException {
        URL url = new URL(headUrl);
        HttpURLConnection headConnection = (HttpURLConnection) url.openConnection();
        headConnection.setRequestMethod("HEAD");
        enableBasicAuthIfRequired(headConnection, url);
        if (headConnection.getHeaderField("Accept-Ranges").equals("bytes")) {
            logger.info("Byte request ranges accepted for this download");
            allowedByteRangeRequests = true;
        }
        totalFileSize = headConnection.getContentLengthLong();
        headConnection.disconnect();
    }

    @Override
    public int read() throws IOException {
        if (totalRead == totalFileSize) {
            return -1;
        }
        if (bufferPointer == maxBufferPointer) {
            fillBuffer();
        }
        int value = buffer[bufferPointer] & 255;
        bufferPointer++;
        totalRead++;
        return value;
    }

    private void fillBuffer() throws IOException {
        List<Exception> exceptions = new ArrayList<>();
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                if (connection == null) {
                    connect();
                }
                maxBufferPointer = httpInputStream.read(buffer);
                bufferPointer = 0;
                if (maxBufferPointer >= 0) {
                    return;
                }
            } catch (IOException e) {
                logger.info(e.getMessage());
                exceptions.add(e);
            }
            // input stream was closed before receiving all the data
            closeConnection();
        }

        for (Exception exception : exceptions) {
            logger.error(exception.getMessage(), exception);
        }
        throw new MaxRetryOnConnectionReached();
    }

    private void connect() throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        if (allowedByteRangeRequests) {
            connection.setRequestProperty("Range", "bytes=" + totalRead + "-");
        }
        enableBasicAuthIfRequired(connection, url);
        httpInputStream = connection.getInputStream();
        if(!allowedByteRangeRequests){
            if(httpInputStream.skip(totalRead)!=totalRead){
                throw new IOException("Could not resume by skipping bytes");
            }
        }
    }

    private void closeConnection() throws IOException {
        if (httpInputStream != null) {
            httpInputStream.close();
            httpInputStream = null;
        }
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    private void enableBasicAuthIfRequired(HttpURLConnection connection, URL url) {
        if (url.getUserInfo() != null) {
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(url.getUserInfo().getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
        }
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }

    public long doReadHeadOfLink() {
        return totalFileSize;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
