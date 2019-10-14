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

import java.util.List;

/**
 * Model class to map success response received from server.
 * Move class to common library if needed.
 */
public class FireObjectResponse {

    private long objectId;
    private String fireOid;
    private long objectSize;
    private String createTime;
    private List<KeyValue> metadata;
    private FilesystemEntry filesystemEntry;

    public FireObjectResponse() {
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public String getFireOid() {
        return fireOid;
    }

    public void setFireOid(String fireOid) {
        this.fireOid = fireOid;
    }

    public long getObjectSize() {
        return objectSize;
    }

    public void setObjectSize(long objectSize) {
        this.objectSize = objectSize;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public List<KeyValue> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<KeyValue> metadata) {
        this.metadata = metadata;
    }

    public FilesystemEntry getFilesystemEntry() {
        return filesystemEntry;
    }

    public void setFilesystemEntry(FilesystemEntry filesystemEntry) {
        this.filesystemEntry = filesystemEntry;
    }

    @Override
    public String toString() {
        return "FireObjectResponse{" +
                "objectId=" + objectId +
                ", fireOid='" + fireOid + '\'' +
                ", objectSize=" + objectSize +
                ", createTime='" + createTime + '\'' +
                ", metadata=" + metadata +
                ", filesystemEntry=" + filesystemEntry +
                '}';
    }
}
