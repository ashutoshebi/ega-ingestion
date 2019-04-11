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
package uk.ac.ebi.ega.ingestion.file.manager.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxAssignation;

import java.time.LocalDateTime;

public class DownloadBoxAssignationValidator implements Validator {

    @Override
    public boolean supports(Class<?> aClass) {
        return DownloadBoxAssignation.class.equals(aClass);
    }

    @Override
    public void validate(Object object, Errors errors) {
        DownloadBoxAssignation downloadBoxAssignation = (DownloadBoxAssignation) object;
        if (downloadBoxAssignation.getUntilDate() != null &&
                LocalDateTime.now().isAfter(downloadBoxAssignation.getUntilDate())) {
            errors.rejectValue("untilDate", "download.box.until.date", "Until date must be in the future");
        }
    }
}
