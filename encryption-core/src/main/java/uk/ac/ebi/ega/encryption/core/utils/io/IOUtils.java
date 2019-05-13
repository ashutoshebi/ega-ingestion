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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IOUtils {

    public static long bufferedPipe(InputStream inputStream, OutputStream outputStream, int bufferSize)
            throws IOException {
        byte[] buffer = new byte[bufferSize];
        int bytesRead = inputStream.read(buffer);
        long totalBytes = 0;
        while (bytesRead != -1) {
            totalBytes += bytesRead;
            outputStream.write(buffer, 0, bytesRead);
            bytesRead = inputStream.read(buffer);
        }
        return totalBytes;
    }

    public static byte[] convertToBytes(char[] password) {
        CharBuffer charBuffer = CharBuffer.wrap(password);
        ByteBuffer byteBuffer = UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), 0, byteBuffer.limit());
        // Clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }

    public static void skipFully(InputStream inputStream, long totalBytes) throws IOException {
        while (totalBytes > 0) {
            totalBytes -= inputStream.skip(totalBytes);
        }
    }

    public static char[] toCharArray(InputStream input, Charset encoding) throws IOException {
        final CharArrayWriter writer = new CharArrayWriter();
        final InputStreamReader reader = new InputStreamReader(input, encoding);
        copy(reader, writer);
        return writer.toCharArray();
    }

    private static void copy(InputStreamReader reader, CharArrayWriter writer) throws IOException {
        char[] buffer = new char[1024];
        int totalRead;
        while ((totalRead = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, totalRead);
        }
    }

}
