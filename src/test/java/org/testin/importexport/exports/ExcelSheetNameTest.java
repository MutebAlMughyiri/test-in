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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ExcelSheetNameTest {

    @Test
    public void aNameExcelAcceptsIsLeftAlone() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                assertEquals(ExportExcel.uniqueSheetName(workbook, "Login"), "Login");
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    // Rule-SHARE-016
    @Test
    public void aSecondShortSheetOfTheSameNameIsNumbered() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                workbook.createSheet(ExportExcel.uniqueSheetName(workbook, "A/B"));

                assertEquals(ExportExcel.uniqueSheetName(workbook, "A*B"), "A_B (2)",
                        "both sanitize to A_B, so the second one takes a number");
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    // Rule-SHARE-016
    @Test
    public void theFirstSheetKeepsTheNameAndTheRestAreNumbered() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                final @NotNull List<String> names = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    final @NotNull String name = ExportExcel.uniqueSheetName(workbook, "Login");
                    workbook.createSheet(name);
                    names.add(name);
                }

                assertEquals(names, List.of("Login", "Login (2)", "Login (3)"));
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void namesAtTheLimitStillGetDistinctSheets() {
        try {
            final String tooLong = "Regression suite for the checkout flow";

            try (Workbook workbook = new XSSFWorkbook()) {
                for (int i = 0; i < 5; i++) {
                    final String name = ExportExcel.uniqueSheetName(workbook, tooLong);

                    assertFalse(name.isBlank(), "a sheet needs a name");
                    assertTrue(name.length() <= 31, "Excel refuses more than 31 characters: " + name);
                    assertNull(workbook.getSheet(name), "name " + i + " was already taken: " + name);

                    workbook.createSheet(name);
                }

                assertEquals(workbook.getNumberOfSheets(), 5);
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void twoNamesDifferingOnlyInCapitalsAreStillTwoSheets() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                workbook.createSheet(ExportExcel.uniqueSheetName(workbook, "Login"));

                final String second = ExportExcel.uniqueSheetName(workbook, "login");
                assertNotEquals(second.toLowerCase(Locale.ROOT), "login", "Excel refuses two sheets differing only in capitals");

                workbook.createSheet(second);
                assertEquals(workbook.getNumberOfSheets(), 2);
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void theCharactersExcelRefusesAreReplaced() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                final String name = ExportExcel.uniqueSheetName(workbook, "'Smoke: run [1]?");

                workbook.createSheet(name);
                assertEquals(workbook.getNumberOfSheets(), 1, "POI accepted the name");
                assertFalse(name.contains(":"), name);
                assertFalse(name.startsWith("'"), name);
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }
}
