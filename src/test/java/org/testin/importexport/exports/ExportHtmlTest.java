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
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class ExportHtmlTest {

    private static @NotNull TestCase aTestCase(final @NotNull String description) {
        return TestCase.builder().uuid(UUID.randomUUID()).description(description).order("m").build();
    }

    private static int count(final @NotNull String text, final @NotNull String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    // UC-SHARE-002, Rule-SHARE-014
    @Test
    public void eachSheetIsOneHeadingAndOneTableThenATotalAndAnEmptyOneIsLeftOut() {
        final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
        sheets.put("Login", List.of(aTestCase("Log in <as> an admin"), aTestCase("Log out")));
        sheets.put("Empty", List.of());
        sheets.put("Payments", List.of(aTestCase("Pay by card")));

        final @NotNull String html = ExportHtml.htmlOf(sheets);

        assertEquals(count(html, "<h2>"), 2, "each sheet that holds test cases is not one heading");
        assertEquals(count(html, "<table>"), 2, "each sheet that holds test cases is not one table");
        assertTrue(html.contains("<h2>Login</h2>") && html.contains("<h2>Payments</h2>"), "a sheet's heading is not its name");
        assertFalse(html.contains("<h2>Empty</h2>"), "a sheet with no test cases was written");
        assertTrue(html.contains("Log in &lt;as&gt; an admin"), "a test case's value was written unescaped");
        assertTrue(html.contains(Bundle.message("export.html.total", "3")), "the total does not count every test case written");
    }

    // UC-SHARE-002
    @Test
    public void theFileHoldsThePageAndAFileThatCannotBeWrittenFailsTheExport() {
        try {
            final @NotNull Path folder = Files.createTempDirectory("testin-export");
            try {
                final @NotNull Path file = folder.resolve("Login.html");
                new ExportHtml().exportToFile(file.toFile(), Map.of("Login", List.of(aTestCase("Log out"))));

                assertTrue(Files.readString(file).contains("<h2>Login</h2>"), "the file does not hold the page");
                expectThrows(UncheckedIOException.class, () -> new ExportHtml().exportToFile(folder.toFile(), Map.of()));
            } finally {
                FileUtil.delete(folder.toFile());
            }
        } catch (final IOException ex) {
            throw new AssertionError("could not make or read the export's folder: " + ex.getMessage(), ex);
        }
    }
}
