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
package uk.ac.ebi.ega.file.re.encryption.processor.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtils {

    public static File moveFile(File absolutePathFile, Path rootPath, String... relativePaths) throws IOException {
        Path path = rootPath;
        for (String relativePath : relativePaths) {
            path = path.resolve(relativePath);
        }
        if (path.toFile().exists()) {
            throw new FileAlreadyExistsException(path.toString());
        }
        return Files.move(absolutePathFile.toPath(), path, StandardCopyOption.ATOMIC_MOVE).toFile();
    }
}
