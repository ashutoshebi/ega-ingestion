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
package uk.ac.ebi.ega.jobs.core.services;

import java.util.Objects;

public class JobDefinition {

    private String name;

    private final Class<?> parameterClass;

    public JobDefinition(String name, Class<?> parameterClass) {
        this.name = Objects.requireNonNull(name);
        this.parameterClass = Objects.requireNonNull(parameterClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobDefinition)) return false;
        JobDefinition that = (JobDefinition) o;
        return name.equals(that.name) &&
                parameterClass.equals(that.parameterClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameterClass);
    }

}
