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

package org.testin.editor.grid;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class GridEditConfirmationTest {

    private static final Path GRID = Paths.get("src", "main", "java", "org", "testin", "editor", "grid");

    private static final String PARENT = "AbstractGridEditListener";

    private static @NotNull List<Path> gridEditListeners() {
        try (Stream<Path> files = Files.list(GRID)) {
            final List<Path> found = new ArrayList<>();

            for (final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().startsWith(PARENT)) continue;

                final String source = Files.readString(file);
                if (source.contains("implements TableModelListener") || source.contains("extends " + PARENT))
                    found.add(file);
            }

            return found;
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + GRID.toAbsolutePath(), ex);
        }
    }

    private static @NotNull String sourceOf(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }
    }

    @Test
    public void bothGridsHaveAnEditListenerToCheck() {
        assertEquals(gridEditListeners().size(), 2,
                "the test case grid and the test run grid, and this test needs updating if a third arrives");
    }

    @Test
    public void everyGridEditListenerGoesThroughTheSharedOne() {
        for (final Path listener : gridEditListeners()) {
            assertTrue(sourceOf(listener).contains("extends " + PARENT),
                    listener.getFileName() + " writes test data from a grid cell without the guards or the "
                            + "confirmation that " + PARENT + " owns");
        }
    }

    @Test
    public void noListenerConfirmsForItself() {
        for (final Path listener : gridEditListeners()) {
            if (!sourceOf(listener).contains("softShow")) continue;

            fail(listener.getFileName() + " says the edit landed in its own words. Two copies of that is how "
                    + "the two grids end up telling a tester different things about the same act - it belongs "
                    + "to " + PARENT + ", which both of them already run through.");
        }
    }

    @Test
    public void theSharedOneSaysWhatTheUpdateDialogSays() {
        final String parent = sourceOf(GRID.resolve(PARENT + ".java"));
        final String dialog = sourceOf(Paths.get("src", "main", "java", "org", "testin", "testcase",
                "UpdateTestCaseFields.java"));

        assertTrue(parent.contains("Done.UPDATED"),
                PARENT + " confirms a grid edit in words of its own rather than naming the outcome");

        assertTrue(dialog.contains("Done.UPDATED"),
                "the update dialog names a different outcome from the grid edit, for the same act on the "
                        + "same field - a tester should not have to learn that they are called different things");
    }
}
