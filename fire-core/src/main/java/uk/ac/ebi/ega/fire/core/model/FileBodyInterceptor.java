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
package uk.ac.ebi.ega.fire.core.model;

import org.apache.http.entity.mime.content.FileBody;
import uk.ac.ebi.ega.fire.core.listener.ProgressListener;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Model class to extend FileBody to provide no. of bytes processed/transferred.
 */
public class FileBodyInterceptor extends FileBody {
    private long bytesWritten;
    private ProgressListener progressListener;

    public FileBodyInterceptor(final File file, final ProgressListener progressListener) {
        super(file);
        this.progressListener = progressListener;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        super.writeTo(new FilterOutputStream(out) {

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                super.write(b, off, len);
                bytesWritten += len;
                progressListener.uploadProgress(bytesWritten);
            }
        });
    }

    public long getBytesWritten() {
        return bytesWritten;
    }
}
