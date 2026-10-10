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
package org.testin;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class NamesSayTheBusinessWordTest {

    private static final @NotNull List<Path> ROOTS = List.of(Path.of("src"), Path.of("testin-java", "src"), Path.of("testin-testng", "src"), Path.of("testin-apimodel", "src"));

    private static final @NotNull Pattern BARE = Pattern.compile("(?<!Test)Run(?!Item|ner|ning|nable)|(?<!Test)(?<!Use)Cases?(?![a-z])|Verdict");

    private static @NotNull List<String> bareTypeNames() {
        final @NotNull List<String> found = new ArrayList<>();

        for (final Path root : ROOTS) {
            assertTrue(Files.isDirectory(root), root + " is missing - this test expects to run from the project root");

            try (Stream<Path> files = Files.walk(root)) {
                files.map(file -> file.getFileName().toString())
                        .filter(name -> name.endsWith(".java"))
                        .map(name -> name.substring(0, name.length() - ".java".length()))
                        .filter(name -> BARE.matcher(name).find())
                        .forEach(found::add);
            } catch (final IOException ex) {
                throw new AssertionError("could not walk " + root, ex);
            }
        }

        return found;
    }

    @Test
    public void noTypeIsNamedWithABareRunCaseOrVerdict() {
        assertEquals(bareTypeNames(), List.of(),
                "A type name says the business word in full: Test Run or Run Item, never Run alone; Test Case, never Case;"
                        + " Run Item Status, never Verdict. Executing automated test methods is Execution.");
    }

    @Test
    public void theScanTellsTheFullWordFromTheBareOne() {
        assertTrue(BARE.matcher("RunEditor").find());
        assertTrue(BARE.matcher("NextCaseAction").find());
        assertTrue(BARE.matcher("VerdictDonut").find());
        assertFalse(BARE.matcher("TestRunEditor").find());
        assertFalse(BARE.matcher("RunItemStatus").find());
        assertFalse(BARE.matcher("TestCaseCard").find());
        assertFalse(BARE.matcher("TestNGRunner").find(), "a runner is a tool, not a test run");
    }
}
