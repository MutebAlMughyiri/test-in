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
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.testcase.Can;
import org.testin.testcase.ImportedRow;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.FailureText;
import org.testin.util.SeparatedValues;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ImportCsv {
    // UC-SHARE-006
    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project project, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        final @NotNull List<TestCaseDto> testCases = parseFile(project, file);
        if (!testCases.isEmpty()) {
            final @NotNull String name = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
            result.put(name, testCases);
        }
        return result;
    }

    public @NotNull List<TestCaseDto> parseFile(final @NotNull Project project, final @NotNull File file) {
        return parseCsvFile(file, project);
    }

    private @NotNull Map<String, Integer> headerIndexes(final @NotNull List<String> headers) {
        final @NotNull Map<String, Integer> byName = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            final @NotNull String headerName = headers.get(i).trim();
            for (final TestSetEditorFields field : TestSetEditorFields.all(Can.IMPORT)) {
                if (field.isColumn(headerName)) byName.put(field.getName().toLowerCase(Locale.ROOT), i);
            }
        }

        return byName;
    }

    private @NotNull List<TestCaseDto> parseCsvFile(final @NotNull File file, final @NotNull Project project) {
        final @NotNull List<TestCaseDto> result = new ArrayList<>();
        final @NotNull List<List<String>> records = parseCsvRecords(file);

        if (records.isEmpty()) return result;

        final @NotNull Map<String, Integer> headerIndexMap = headerIndexes(records.getFirst());

        int refused = 0;

        for (int r = 1; r < records.size(); r++) {
            final @NotNull List<String> values = records.get(r);

            if (values.stream().allMatch(String::isBlank)) continue;

            final @NotNull ImportedRow imported = TestSetEditorFields.importRow(project, field -> Optional.ofNullable(headerIndexMap.get(field.getName().toLowerCase(Locale.ROOT)))
                    .filter(colIndex -> colIndex < values.size())
                    .map(colIndex -> values.get(colIndex).trim())
                    .orElse(""));

            refused += imported.refused();
            result.add(imported.testCase());
        }

        TestSetEditorFields.sayWhatWasRefused(project, refused);

        return result;
    }

    // UC-SHARE-005, Rule-SHARE-124
    private @NotNull List<List<String>> parseCsvRecords(final @NotNull File file) {
        try {
            final @NotNull String text = Files.readString(file.toPath());
            return SeparatedValues.split(text, ',').stream()
                    .filter(fields -> !fields.stream().allMatch(String::isEmpty))
                    .toList();
        } catch (final IOException ex) {
            TestinLogger.error("CSV parse failed: " + FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }
}
