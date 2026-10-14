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

import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class OwnWritesTest {

    private static final @NotNull String THIS_WINDOW = "window-a";

    private static final @NotNull String ANOTHER_WINDOW = "window-b";

    private static final byte @NotNull [] OURS = "{\"description\":\"Log in\"}".getBytes(StandardCharsets.UTF_8);

    private static final byte @NotNull [] THEIRS = "{\"description\":\"Log in as admin\"}".getBytes(StandardCharsets.UTF_8);

    private static @NotNull Path tempFile() {
        try {
            final @NotNull Path file = Files.createTempFile("testin-own-writes", ".json");
            Files.write(file, OURS);
            return file;
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a file to watch: " + ex.getMessage(), ex);
        }
    }

    private static void testerEdits(final @NotNull Path file) {
        try {
            Files.write(file, THEIRS);
        } catch (final IOException ex) {
            throw new AssertionError("Could not stand in for the tester's edit: " + ex.getMessage(), ex);
        }
    }

    private static void delete(final @NotNull Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException ex) {
            TestinLogger.warn("Could not clean up " + file + ": " + ex.getMessage());
        }
    }

    @Test
    public void ourOwnSaveIsIgnored() {
        final @NotNull Path file = tempFile();

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(THIS_WINDOW, file);
            ours.wrote(THIS_WINDOW, file, OURS);

            assertTrue(ours.areOurs(file, THIS_WINDOW), "the plugin's own save must not rebuild the tree under the tester who caused it");
        } finally {
            delete(file);
        }
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019
    @Test
    public void anotherWindowStillHearsTheWrite() {
        final @NotNull Path file = tempFile();

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(THIS_WINDOW, file);
            ours.wrote(THIS_WINDOW, file, OURS);

            assertFalse(ours.areOurs(file, ANOTHER_WINDOW),
                    "a second window on the same test project has not seen this write, so the event is its news to read");
        } finally {
            delete(file);
        }
    }

    // Rule-INTERNAL-064
    @Test
    public void aHandEditInsideTheWindowIsNotIgnored() {
        final @NotNull Path file = tempFile();

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(THIS_WINDOW, file);
            ours.wrote(THIS_WINDOW, file, OURS);

            testerEdits(file);

            assertFalse(ours.areOurs(file, THIS_WINDOW),
                    "a tester edited this file after the plugin wrote it, inside the window - their edit must reach the screen");
        } finally {
            delete(file);
        }
    }

    @Test
    public void aWriteStillInFlightIsOurs() {
        final @NotNull Path file = tempFile();

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(THIS_WINDOW, file);

            assertTrue(ours.areOurs(file, THIS_WINDOW), "the claim is made before the write, so an event during it is still ours");
        } finally {
            delete(file);
        }
    }

    @Test
    public void aFileNobodyClaimedIsNeverOurs() {
        final @NotNull Path file = tempFile();

        try {
            assertFalse(new OwnWrites().areOurs(file, THIS_WINDOW));
        } finally {
            delete(file);
        }
    }
}
