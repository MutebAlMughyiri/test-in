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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

public class ImportExcelIdeTest extends AbstractTempRootIdeTest {

    private static void sheetOf(final @NotNull Workbook workbook, final @NotNull String name, final @NotNull String... descriptions) {
        final @NotNull Sheet sheet = workbook.createSheet(name);
        sheet.createRow(0).createCell(0).setCellValue("Description");
        for (int i = 0; i < descriptions.length; i++)
            sheet.createRow(i + 1).createCell(0).setCellValue(descriptions[i]);
    }

    // UC-SHARE-006
    public void testAHiddenSheetAndASheetOfOnlyItsHeadingsAreNotImported() {
        final @NotNull File file = root.resolve("test cases.xlsx").toFile();
        try (final Workbook workbook = new XSSFWorkbook(); final OutputStream out = Files.newOutputStream(file.toPath())) {
            sheetOf(workbook, "Login", "Log in with a valid user");
            sheetOf(workbook, "Old", "Log in with a password");
            sheetOf(workbook, "Archive", "Log out");
            sheetOf(workbook, "Empty");
            workbook.setSheetHidden(1, true);
            workbook.setSheetVisibility(2, SheetVisibility.VERY_HIDDEN);
            workbook.write(out);
        } catch (final IOException ex) {
            throw new AssertionError("could not write the workbook to import", ex);
        }

        assertEquals("a hidden sheet, or one of only its headings, was imported", List.of("Login"), List.copyOf(new ImportExcel().processImport(getProject(), file).keySet()));
    }
}
