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

package org.testin.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.model.FileKind;
import org.testin.model.testrun.RunItem;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RunItemsSurviveRenameTest {

    private static final @NotNull UUID JUDGED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final @NotNull String A_RESULT = """
            {
              "id" : "11111111-1111-4111-8111-111111111101",
              "executions" : [ {
                "status" : "PASSED",
                "actualResult" : "Signed in and reached the dashboard."
              } ]
            }""";

    private static @NotNull Path createTestRun(final @NotNull Path folder) {
        write(folder.resolve(".testrun"), "{}");
        write(folder.resolve(FileKind.RUN_ITEM.fileName(JUDGED_TEST_CASE)), A_RESULT);
        return folder;
    }

    private static @NotNull Path tempRoot() {
        try {
            return Files.createTempDirectory("testin-rename");
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a temporary directory to rename in: " + ex.getMessage(), ex);
        }
    }

    private static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull Path move(final @NotNull Path from, final @NotNull Path to) {
        try {
            return Files.move(from, to);
        } catch (final IOException ex) {
            throw new AssertionError("Could not rename " + from + " to " + to + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull RunItem read(final @NotNull Path file) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).readValue(file.toFile(), RunItem.class);
        } catch (final IOException ex) {
            throw new AssertionError("The run item at " + file + " no longer parses: " + ex.getMessage(), ex);
        }
    }

    @Test
    public void aRunItemIsNamedByItsTestCaseAndNotByItsFolder() {
        assertEquals(FileKind.RUN_ITEM.fileName(JUDGED_TEST_CASE), JUDGED_TEST_CASE + ".runitem",
                "A result is named by the test case it is about, so there is no folder name in it to keep in step."
                        + " The moment a result's name is derived from its folder, renaming a test run empties it.");
    }

    @Test
    public void renamingTheFolderKeepsTheRunItemsWhereTheScanLooks() {
        final @NotNull Path root = tempRoot();

        try {
            final @NotNull Path cycle = createTestRun(root.resolve("Cycle-1"));
            final @NotNull Path renamed = move(cycle, root.resolve("Cycle-1 (rerun)"));

            final @NotNull Path result = renamed.resolve(FileKind.RUN_ITEM.fileName(JUDGED_TEST_CASE));
            assertTrue(Files.exists(result),
                    "After a rename the scan looks for " + result.getFileName() + " and it has to be there,"
                            + " or every run item status in the test run is gone at the next index");

            assertEquals(read(result).getStatus().name(), "PASSED",
                    "The run item status the tester recorded before the rename is the thing this issue was about");

            assertTrue(Files.exists(renamed.resolve(".testrun")), "The marker moved with the folder, as it always did");
        } finally {
            TempTree.delete(root);
        }
    }
}
