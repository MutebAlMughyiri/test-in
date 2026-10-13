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

package org.testin.importexport.exports;

import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.FailureText;
import org.testin.util.SeparatedValues;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportCsv {
    // UC-SHARE-002
    public void exportToFile(final @NotNull File destFile, final @NotNull Map<String, List<TestCase>> sheetsData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            final @NotNull List<String> headerNames = TestSetEditorFields.all(Can.EXPORT).stream()
                    .map(TestSetEditorFields::getName)
                    .toList();

            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (final Map.Entry<String, List<TestCase>> entry : sheetsData.entrySet()) {
                for (final TestCase testCase : entry.getValue()) {
                    final @NotNull List<String> rowValues = new ArrayList<>();
                    for (final TestSetEditorFields field : TestSetEditorFields.all(Can.EXPORT)) {
                        rowValues.add(SeparatedValues.field(field.gridValue(testCase), ','));
                    }
                    writer.write(String.join(",", rowValues));
                    writer.newLine();
                }
            }
        } catch (final IOException ex) {
            TestinLogger.error(FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }
}
