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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ScanProgressTest {

    private static final @NotNull Path SOURCE_ROOT = Paths.get("src", "main", "java", "org", "testin", "indexer");

    private static @NotNull String read(final @NotNull String fileName) {
        final @NotNull Path file = SOURCE_ROOT.resolve(fileName);

        try {
            return Files.readString(file);
        } catch (final IOException notThere) {
            fail("Could not read " + file.toAbsolutePath() + ": " + notThere.getMessage());
            return "";
        }
    }

    @Test
    public void theScanCanBeGivenAProgressBar() {
        try {
            assertNotNull(ProjectIndexer.class.getDeclaredMethod("scanSingleProject", Path.class, ProgressIndicator.class));
        } catch (final NoSuchMethodException missing) {
            fail("ProjectIndexer has no scanSingleProject(Path, ProgressIndicator): "
                    + "a caller with a progress bar has nowhere to hand it, so the bar reports nothing");
        }
    }

    @Test
    public void theRescanHandsItsIndicatorToTheScan() {
        assertTrue(read("Rescan.java").contains("rescanChangedProject(testProject, indicator)"),
                "Rescan builds a progress bar and must hand it to the rescan; calling it without the bar "
                        + "leaves the tester watching a bar that names nothing and cannot be stopped");

        assertTrue(read("ProjectIndexer.java").contains("scanSingleProject(projectPath, indicator)"),
                "rescanChangedProject must hand the rescan's progress bar to the scan, not start one of its own");
    }

    // Rule-INTERNAL-013
    @Test
    public void bothScanLoopsStopWhenTheTesterCancels() {
        final @NotNull String source = read("IndexingScanner.java");
        final int checks = StringUtil.getOccurrenceCount(source, "indicator.isCanceled()");

        assertTrue(checks >= 2,
                "the test-set loop and the test-run loop must each check indicator.isCanceled(), found "
                        + checks + ": a scan that never asks turns Cancel into a button that does nothing");
    }
}
