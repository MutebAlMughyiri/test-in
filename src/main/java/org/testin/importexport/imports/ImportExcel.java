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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.testcase.Can;
import org.testin.testcase.ImportedRow;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.FailureText;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ImportExcel {
    private static boolean isEmpty(final @NotNull Row row, final @NotNull DataFormatter dataFormatter) {
        for (int column = 0; column < row.getLastCellNum(); column++) {
            if (!dataFormatter.formatCellValue(row.getCell(column)).trim().isEmpty()) return false;
        }
        return true;
    }

    // UC-SHARE-006
    public @NotNull Map<String, List<TestCase>> processImport(final @NotNull Project project, final @NotNull File file) {
        final @NotNull Map<String, List<TestCase>> result = new LinkedHashMap<>(parseFile(project, file));

        TestinLogger.info("Import: parsed " + result.values().stream().mapToInt(List::size).sum()
                + " test cases from " + result.size() + " sheet(s) of " + file.getName());
        return result;
    }

    public @NotNull Map<String, List<TestCase>> parseFile(final @NotNull Project project, final @NotNull File file) {
        final @NotNull Map<String, List<TestCase>> result = new LinkedHashMap<>();
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            parseWorkbook(workbook, project, result);

        } catch (final IOException ex) {
            TestinLogger.error(FailureText.of(ex));
            throw new RuntimeException(ex);
        }
        return result;
    }

    // UC-SHARE-005, Rule-SHARE-106
    private void parseWorkbook(final @NotNull Workbook workbook, final @NotNull Project project, final @NotNull Map<String, List<TestCase>> result) {
        final @NotNull DataFormatter dataFormatter = new DataFormatter();
        int refused = 0;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) continue;

            final @NotNull Sheet sheet = workbook.getSheetAt(i);
            final @NotNull ParsedTestCases parsed = parseSheet(project, sheet, dataFormatter);
            refused += parsed.refused();

            if (!parsed.testCases().isEmpty()) {
                result.put(sheet.getSheetName(), parsed.testCases());
            }
        }

        // Rule-SHARE-106
        TestSetEditorFields.sayWhatWasRefused(project, refused);
    }

    private @NotNull ParsedTestCases parseSheet(final @NotNull Project project, final @NotNull Sheet sheet, final @NotNull DataFormatter dataFormatter) {
        return Optional.ofNullable(sheet.getRow(0))
                .map(headerRow -> readRows(project, sheet, headerRow, dataFormatter))
                .orElse(ParsedTestCases.NOTHING);
    }

    private @NotNull ParsedTestCases readRows(final @NotNull Project project, final @NotNull Sheet sheet, final @NotNull Row headerRow, final @NotNull DataFormatter dataFormatter) {
        final @NotNull Map<String, Integer> headerIndexMap = new HashMap<>();
        for (final Cell cell : headerRow) {
            final @NotNull String headerName = dataFormatter.formatCellValue(cell).trim();
            for (final TestSetEditorFields importedField : TestSetEditorFields.all(Can.IMPORT)) {
                if (importedField.isColumn(headerName)) {
                    headerIndexMap.put(importedField.getName().toLowerCase(Locale.ROOT), cell.getColumnIndex());
                }
            }
        }

        final @NotNull List<TestCase> sheetList = new ArrayList<>();

        int refused = 0;

        for (final Row row : sheet) {
            if (row.getRowNum() == headerRow.getRowNum() || isEmpty(row, dataFormatter)) continue;

            final @NotNull ImportedRow imported = TestSetEditorFields.importRow(project, field -> Optional.ofNullable(headerIndexMap.get(field.getName().toLowerCase(Locale.ROOT)))
                    .map(columnIndex -> dataFormatter.formatCellValue(row.getCell(columnIndex)).trim())
                    .orElse(""));

            refused += imported.refused();
            sheetList.add(imported.testCase());
        }

        return new ParsedTestCases(sheetList, refused);
    }
}
