/*
 * Copyright 2026 Muteb AlMughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.importexport.imports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.testcase.ImportedRow;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.SeparatedValues;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ImportCsv {
    // UC-SHARE-006
    public @NotNull Map<String, List<TestCase>> processImport(final @NotNull Project project, final @NotNull File file) {
        final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
        final @NotNull List<TestCase> testCases = parseFile(project, file);
        if (!testCases.isEmpty()) {
            final @NotNull String name = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
            sheets.put(name, testCases);
        }
        return sheets;
    }

    public @NotNull List<TestCase> parseFile(final @NotNull Project project, final @NotNull File file) {
        final @NotNull List<TestCase> testCases = new ArrayList<>();
        final @NotNull List<List<String>> records = parseCsvRecords(file);

        if (records.isEmpty()) return testCases;

        final @NotNull Map<TestSetEditorFields, Integer> columns = TestSetEditorFields.columnsOf(records.getFirst());

        int refused = 0;

        for (int row = 1; row < records.size(); row++) {
            final @NotNull List<String> values = records.get(row);

            if (values.stream().allMatch(String::isBlank)) continue;

            final @NotNull ImportedRow imported = TestSetEditorFields.importRow(project, field -> Optional.ofNullable(columns.get(field))
                    .filter(columnIndex -> columnIndex < values.size())
                    .map(columnIndex -> values.get(columnIndex).trim())
                    .orElse(""));

            refused += imported.refused();
            testCases.add(imported.testCase());
        }

        TestSetEditorFields.sayWhatWasRefused(project, refused);

        return testCases;
    }

    // UC-SHARE-005, Rule-SHARE-124
    private @NotNull List<List<String>> parseCsvRecords(final @NotNull File file) {
        try {
            final @NotNull String text = Files.readString(file.toPath());
            return SeparatedValues.split(text, ',').stream()
                    .filter(fields -> !fields.stream().allMatch(String::isEmpty))
                    .toList();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
