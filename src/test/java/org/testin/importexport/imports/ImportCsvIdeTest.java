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
import org.testin.model.TestCaseDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportCsvIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String BOM = Character.toString(0xFEFF);


    private @NotNull List<TestCaseDto> imported(final @NotNull String text) {
        try {
            final @NotNull Path file = root.resolve("test cases.csv");
            Files.writeString(file, text, StandardCharsets.UTF_8);

            return new ImportCsv().parseFile(getProject(), file.toFile());
        } catch (final IOException ex) {
            throw new AssertionError("Could not write the file to import: " + ex.getMessage(), ex);
        }
    }

    // UC-SHARE-005, Rule-SHARE-124
    public void testACsvFileIsReadTheWayASpreadsheetWritesIt() {
        final @NotNull List<TestCaseDto> testCases = imported(BOM + """
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

    // UC-SHARE-005, Rule-SHARE-110
    public void testAHeadingIsMatchedWhateverItsCaseAndUnderscores() {
        final @NotNull List<TestCaseDto> testCases = imported("""
                description,EXPECTED_RESULT,pre conditions,Test_Data
                log in with a valid user,the dashboard opens,a user exists,user = muteb
                """);

        assertEquals(1, testCases.size());
        assertEquals("log in with a valid user", testCases.getFirst().getDescription());
        assertEquals("the dashboard opens", testCases.getFirst().getExpectedResult());
        assertEquals("a user exists", testCases.getFirst().getPreConditions());
        assertEquals("user = muteb", testCases.getFirst().getTestData());
    }
}
