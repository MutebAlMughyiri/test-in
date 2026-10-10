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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class OneTypePerFileTest {

    private static final @NotNull List<Path> ROOTS = List.of(Path.of("src", "main", "java"), Path.of("testin-java", "src", "main", "java"), Path.of("testin-testng", "src", "main", "java"), Path.of("testin-apimodel", "src", "main", "java"));

    private static final @NotNull Pattern NESTED = Pattern.compile("^\\s+(?:(?:public|protected|private|static|final|sealed|strictfp)\\s+)*(?:enum\\s+[A-Z]\\w*\\s*(?:\\{|implements\\b)|record\\s+[A-Z]\\w*\\s*[(<])");

    private static @NotNull List<String> nestedIn(final @NotNull Path file) {
        final @NotNull List<String> found = new ArrayList<>();

        try {
            final @NotNull List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

            for (int i = 0; i < lines.size(); i++) {
                if (NESTED.matcher(lines.get(i)).find()) found.add(file + ":" + (i + 1) + "  " + lines.get(i).trim());
            }
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }

        return found;
    }

    private static @NotNull List<String> nestedEnumsAndRecords() {
        final @NotNull List<String> found = new ArrayList<>();

        for (final Path root : ROOTS) {
            assertTrue(Files.isDirectory(root), root + " is missing - this test expects to run from the project root");

            try (Stream<Path> files = Files.walk(root)) {
                files.filter(file -> file.toString().endsWith(".java")).forEach(file -> found.addAll(nestedIn(file)));
            } catch (final IOException ex) {
                throw new AssertionError("could not walk " + root, ex);
            }
        }

        return found;
    }

    @Test
    public void noEnumOrRecordIsDeclaredInsideAnotherType() {
        assertEquals(nestedEnumsAndRecords(), List.of(),
                "An enum or a record is a type other classes find by its name, so it has a file of its own"
                        + " in the package of the class that used to hold it (#381). Move it out, keeping its visibility.");
    }

    @Test
    public void theScanFindsANestedRecordWhenThereIsOne() {
        assertTrue(NESTED.matcher("    private record Work(@NotNull Project p) {").find());
        assertTrue(NESTED.matcher("    public enum Can {").find());
        assertTrue(NESTED.matcher("    record Answer<T>(@NotNull List<T> rows) {").find());
        assertFalse(NESTED.matcher("public record Pill(@NotNull String text) implements Badge {").find(),
                "a top-level record is the one type its file is for");
    }
}
