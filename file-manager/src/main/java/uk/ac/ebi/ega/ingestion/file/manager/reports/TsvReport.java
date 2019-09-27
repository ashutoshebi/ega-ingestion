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
package uk.ac.ebi.ega.ingestion.file.manager.reports;

import joptsimple.internal.Strings;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TsvReport<T> {

    private final String fileName;

    private final List<String> columns;

    private final Function<? super T, String[]> getColumnsFunction;

    public TsvReport(String fileName, List<String> columns, Function<? super T, String[]> getColumnsFunction) {
        this.fileName = fileName;
        this.columns = columns;
        this.getColumnsFunction = getColumnsFunction;
    }

    public void stream(HttpServletResponse response, Stream<? extends T> stream) throws IOException {
        response.setContentType("application/tsv");
        response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setCharacterEncoding("UTF-8");

        try (final PrintWriter out = response.getWriter()) {
            out.write(columns.stream().collect(Collectors.joining("\t")));
            stream.map(getColumnsFunction).map(this::concatenate).forEach(s -> {
                out.write("\n");
                out.write(s);
            });
            out.flush();
        }
    }

    private String concatenate(String[] strings) {
        return Strings.join(strings, "\t");
    }

}
