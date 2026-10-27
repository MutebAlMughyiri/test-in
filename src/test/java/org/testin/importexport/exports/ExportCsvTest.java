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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class ExportCsvTest {

    private static @NotNull TestCase aTestCase(final @NotNull String description) {
        return TestCase.builder().uuid(UUID.randomUUID()).description(description).order("m").build();
    }

    private static void inAFolder(final @NotNull Consumer<Path> test) {
        try {
            final @NotNull Path folder = Files.createTempDirectory("testin-export");
            try {
                test.accept(folder);
            } finally {
                FileUtil.delete(folder.toFile());
            }
        } catch (final IOException ex) {
            throw new AssertionError("could not make the export's folder: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<String> linesOf(final @NotNull Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("could not read the export: " + ex.getMessage(), ex);
        }
    }

    // UC-SHARE-002
    @Test
    public void theFileIsAHeaderOfTheExportedFieldsThenOneRowForEachTestCaseOfEverySheet() {
        inAFolder(folder -> {
            final @NotNull Path file = folder.resolve("Checkout.csv");
            final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
            sheets.put("Login", List.of(aTestCase("Log in, then log out"), aTestCase("Log out")));
            sheets.put("Payments", List.of(aTestCase("Pay by card")));

            new ExportCsv().exportToFile(file.toFile(), sheets);

            final @NotNull List<String> lines = linesOf(file);
            assertEquals(lines.getFirst(), String.join(",", TestSetEditorFields.all(Can.EXPORT).stream().map(TestSetEditorFields::getName).toList()), "the header does not name the exported fields");
            assertEquals(lines.size(), 4, "the file is not a header and one row for each test case of every sheet: " + lines);
            assertTrue(lines.get(1).contains("\"Log in, then log out\""), "a value holding a comma was not quoted: " + lines.get(1));
            assertTrue(lines.get(3).contains("Pay by card"), "the second sheet's test case was not written: " + lines.get(3));
        });
    }

    // UC-SHARE-002
    @Test
    public void aDestinationThatCannotBeWrittenFailsTheExport() {
        inAFolder(folder -> expectThrows(UncheckedIOException.class, () -> new ExportCsv().exportToFile(folder.toFile(), Map.of())));
    }
}
