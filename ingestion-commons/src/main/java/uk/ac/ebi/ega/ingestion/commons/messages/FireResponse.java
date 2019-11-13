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
package uk.ac.ebi.ega.ingestion.commons.messages;

public class FireResponse {

    private String fireOid;
    private String firePath;
    private boolean published;

    public FireResponse() {
    }

    public FireResponse(final String fireOid, final String firePath, final boolean published) {
        this.fireOid = fireOid;
        this.firePath = firePath;
        this.published = published;
    }

    public String getFireOid() {
        return fireOid;
    }

    public void setFireOid(String fireOid) {
        this.fireOid = fireOid;
    }

    public String getFirePath() {
        return firePath;
    }

    public void setFirePath(String firePath) {
        this.firePath = firePath;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    public String toString() {
        return "FireResponse{" +
                "fireOid='" + fireOid + '\'' +
                ", firePath='" + firePath + '\'' +
                ", published=" + published +
                '}';
    }
}
