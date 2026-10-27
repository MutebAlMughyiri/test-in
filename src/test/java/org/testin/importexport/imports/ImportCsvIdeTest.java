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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Said;
import org.testin.importexport.exports.ExportCsv;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.notifications.Refused;
import org.testin.util.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImportCsvIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String BOM = Character.toString(0xFEFF);


    private @NotNull List<TestCase> imported(final @NotNull String text) {
        try {
            final @NotNull Path file = root.resolve("test cases.csv");
            Files.writeString(file, text, StandardCharsets.UTF_8);

            return new ImportCsv().parseFile(getProject(), file.toFile());
        } catch (final IOException ex) {
            throw new AssertionError("Could not write the file to import: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull TestCase aTestCase(final @NotNull String description) {
        return TestCase.builder().uuid(UUID.randomUUID()).description(description).order("m").build();
    }

    // UC-SHARE-005, Rule-SHARE-124
    public void testACsvFileIsReadTheWayASpreadsheetWritesIt() {
        final @NotNull List<TestCase> testCases = imported(BOM + """
                Description,Expected Result,Module\r
                log in with a valid user,"the dashboard opens, and ""Welcome"" shows",accounts\r
                \r
                ,,\r
                log out,the 5" login page opens,accounts\r
                """);

        assertEquals("a line with no value in it is skipped", 2, testCases.size());
        assertEquals("the byte-order mark is not part of the first heading", "log in with a valid user", testCases.getFirst().getDescription());
        assertEquals("a quote at the start opens a quoted value", "the dashboard opens, and \"Welcome\" shows", testCases.getFirst().getExpectedResult());
        assertEquals("a quote anywhere else is kept as typed", "the 5\" login page opens", testCases.getLast().getExpectedResult());
        assertEquals("accounts", testCases.getLast().getModule());
    }

    // UC-SHARE-006, Rule-SHARE-106
    public void testValuesTestinCannotReadAreRefusedAndSaidOnceWithTheirCount() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        final @NotNull List<TestCase> testCases = imported("""
                Description,Priority
                log in with a valid user,Urgent
                log out,Soon
                """);

        assertEquals("a row with a value Testin cannot read was dropped", 2, testCases.size());
        assertEquals("a refused priority did not leave the test case at the lowest priority", Priority.LOW, testCases.getFirst().getPriority());
        assertEquals("the refused values were not said once with their count", List.of(Refused.UNREADABLE.about(Bundle.message("field.value.many", "2"))), said.shown());
    }

    // UC-SHARE-005, Rule-SHARE-110
    public void testAHeadingIsMatchedWhateverItsCapitalsAndUnderscores() {
        final @NotNull List<TestCase> testCases = imported("""
                description,EXPECTED_RESULT,pre conditions,Test_Data
                log in with a valid user,the dashboard opens,a user exists,user = muteb
                """);

        assertEquals(1, testCases.size());
        assertEquals("log in with a valid user", testCases.getFirst().getDescription());
        assertEquals("the dashboard opens", testCases.getFirst().getExpectedResult());
        assertEquals("a user exists", testCases.getFirst().getPreConditions());
        assertEquals("user = muteb", testCases.getFirst().getTestData());
    }

    // UC-SHARE-006
    public void testACsvBecomesOneSheetNamedAfterItsFile() {
        final @NotNull File csv = root.resolve("Login [web].csv").toFile();
        new ExportCsv().exportToFile(csv, Map.of("Login", List.of(aTestCase("Log in with a valid user"), aTestCase("Log out"))));

        final @NotNull Map<String, List<TestCase>> sheets = new ImportCsv().processImport(getProject(), csv);

        assertEquals("the sheet is not named after its file, its brackets made safe", List.of("Login _web_"), List.copyOf(sheets.keySet()));
        assertEquals("the sheet does not hold the file's test cases", List.of("Log in with a valid user", "Log out"), sheets.getOrDefault("Login _web_", List.of()).stream().map(TestCase::getDescription).toList());
    }

    // UC-SHARE-006
    public void testACsvOfOnlyItsHeaderGivesNoSheet() {
        final @NotNull File csv = root.resolve("Empty.csv").toFile();
        new ExportCsv().exportToFile(csv, Map.of());

        assertEquals("a CSV with no test cases gave a sheet", Map.of(), new ImportCsv().processImport(getProject(), csv));
    }

    // UC-SHARE-005, Rule-SHARE-124
    public void testACsvThatCannotBeReadFailsTheImport() {
        final @NotNull File missing = root.resolve("Missing.csv").toFile();

        assertThrows(UncheckedIOException.class, () -> new ImportCsv().processImport(getProject(), missing));
    }
}
