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

package org.testin.importexport;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExportThenImportIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull ZonedDateTime CREATED = ZonedDateTime.of(2026, 8, 20, 3, 33, 24, 0, ZoneId.of("Asia/Riyadh"));

    private static final @NotNull ZonedDateTime UPDATED = ZonedDateTime.of(2026, 9, 14, 11, 30, 5, 0, ZoneId.of("Asia/Riyadh"));

    private static @NotNull TestCase aTestCase(final @NotNull String description) {
        return TestCase.builder()
                .description(description)
                .expectedResult("the dashboard opens")
                .createdBy("Sara")
                .createdAt(CREATED)
                .updatedBy("Muteb")
                .updatedAt(UPDATED)
                .build();
    }

    private static @NotNull List<String> descriptions(final @NotNull List<TestCase> testCases) {
        return testCases.stream().map(TestCase::getDescription).toList();
    }

    private static @NotNull Map<String, List<String>> descriptionsBySheet(final @NotNull Map<String, List<TestCase>> sheets) {
        final @NotNull Map<String, List<String>> descriptions = new LinkedHashMap<>();
        sheets.forEach((sheet, testCases) -> descriptions.put(sheet, descriptions(testCases)));
        return descriptions;
    }

    private @NotNull Map<String, List<TestCase>> roundTrip(final @NotNull Map<String, List<TestCase>> sheets) {
        final @NotNull File file = root.resolve("export.xlsx").toFile();
        FileFormat.XLSX.exportToFile(getProject(), file, sheets);

        return FileFormat.XLSX.importToFile(getProject(), file);
    }

    // UC-SHARE-002, Rule-SHARE-007, Rule-SHARE-012
    public void testEachTestSetIsOneSheetNamedAfterItselfWithItsRowsInOrder() {
        final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
        sheets.put("Login", List.of(aTestCase("log in with a valid user"), aTestCase("a wrong password is refused"), aTestCase("a locked account cannot log in")));
        sheets.put("Checkout", List.of(aTestCase("pay with a saved card")));

        final @NotNull Map<String, List<TestCase>> read = roundTrip(sheets);

        assertEquals(List.of("Login", "Checkout"), List.copyOf(read.keySet()));
        assertEquals(descriptions(sheets.get("Login")), descriptions(Objects.requireNonNull(read.get("Login"), "no Login sheet was read")));
        assertEquals(descriptions(sheets.get("Checkout")), descriptions(Objects.requireNonNull(read.get("Checkout"), "no Checkout sheet was read")));
    }

    // UC-SHARE-005, Rule-SHARE-121
    public void testASheetExportedAndImportedUnchangedKeepsItsDates() {
        final @NotNull TestCase read = Objects.requireNonNull(roundTrip(Map.of("Login", List.of(aTestCase("log in with a valid user")))).get("Login"), "no Login sheet was read").getFirst();

        assertEquals(CREATED.toInstant(), read.getCreatedAt().toInstant());
        assertEquals(UPDATED.toInstant(), read.getUpdatedAt().toInstant());
    }

    // UC-SHARE-002, UC-SHARE-005, Rule-SHARE-124
    public void testACsvExportedAndImportedThroughItsFormatKeepsItsTestCases() {
        final @NotNull File file = root.resolve("Login.csv").toFile();
        FileFormat.CSV.exportToFile(getProject(), file, Map.of("Login", List.of(aTestCase("log in with a valid user"), aTestCase("a wrong password is refused"))));

        final @NotNull FileFormat format = FileFormat.importerFor(file.getName()).orElseThrow(() -> new AssertionError("a .csv file has no format to import it"));
        assertEquals(FileFormat.CSV, format);
        assertEquals("the CSV did not come back as the test cases it was exported from", Map.of("Login", List.of("log in with a valid user", "a wrong password is refused")), descriptionsBySheet(format.importToFile(getProject(), file)));
    }

    // UC-SHARE-005, Rule-SHARE-110
    public void testAnOldXlsWorkbookIsReadAsExcel() {
        final @NotNull File file = root.resolve("Login.xls").toFile();
        try (HSSFWorkbook workbook = new HSSFWorkbook(); OutputStream out = Files.newOutputStream(file.toPath())) {
            final @NotNull Sheet sheet = workbook.createSheet("Login");
            sheet.createRow(0).createCell(0).setCellValue("description");
            sheet.getRow(0).createCell(1).setCellValue("EXPECTED_RESULT");
            sheet.createRow(1).createCell(0).setCellValue("log in with a valid user");
            sheet.getRow(1).createCell(1).setCellValue("the dashboard opens");
            workbook.write(out);
        } catch (final IOException ex) {
            throw new AssertionError("could not write the .xls workbook: " + ex.getMessage(), ex);
        }

        final @NotNull FileFormat format = FileFormat.importerFor(file.getName()).orElseThrow(() -> new AssertionError("a .xls file has no format to import it"));
        assertEquals(FileFormat.XLS, format);
        final @NotNull TestCase read = Objects.requireNonNull(format.importToFile(getProject(), file).get("Login"), "no Login sheet was read").getFirst();
        assertEquals("log in with a valid user", read.getDescription());
        assertEquals("the dashboard opens", read.getExpectedResult());
    }

    // UC-SHARE-005, Rule-SHARE-110
    public void testOnlyAFormatReadByItsColumnHeadingsHintsAtThem() {
        final @NotNull String columns = "Description, Expected Result";
        final @NotNull List<FileFormat> hinting = Arrays.stream(FileFormat.values()).filter(format -> !format.hintFor(columns).isBlank()).toList();

        assertEquals("the formats that hint at the column headings are not the two read by them", List.of(FileFormat.XLSX, FileFormat.CSV), hinting);
        hinting.forEach(format -> assertTrue(format.getLabel() + "'s hint does not name the columns", format.hintFor(columns).contains(columns)));
    }
}
