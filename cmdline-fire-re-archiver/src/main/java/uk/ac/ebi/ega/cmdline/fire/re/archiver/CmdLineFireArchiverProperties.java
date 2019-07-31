/*
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
 */
package uk.ac.ebi.ega.cmdline.fire.re.archiver;

public class CmdLineFireArchiverProperties {

    private String stagingPath;

    private String stableIdPrefix;

    public String getStagingPath() {
        return stagingPath;
    }

    public void setStagingPath(final String stagingPath) {
        this.stagingPath = stagingPath;
    }

    public String getStableIdPrefix() {
        return stableIdPrefix;
    }

    public void setStableIdPrefix(final String stableIdPrefix) {
        this.stableIdPrefix = stableIdPrefix;
    }
}
