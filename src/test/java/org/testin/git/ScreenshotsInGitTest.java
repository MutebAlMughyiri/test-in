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

package org.testin.git;

import org.testin.git.change.PendingChangeReader;
import org.testin.model.NodeType;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ScreenshotsInGitTest {

    private static final String RESULT = "runs/cycle38/4fd2a19b-59c7-44df-8cc4-ec5d293b18e9.runitem";
    private static final String ADDED = "runs/cycle38/k3f9a.png";
    private static final String REMOVED = "runs/cycle38/q81zd.png";
    private static final String OTHER_RUN = "runs/cycle39/m4x0c.png";

    private static void markATestRun(final Path root) {
        try {
            final Path at = root.resolve("runs/cycle38");
            Files.createDirectories(at);
            Files.writeString(at.resolve(NodeType.TEST_RUN.getMarker()), "{}");
        } catch (final Exception ex) {
            throw new AssertionError("could not write the test run's marker", ex);
        }
    }

    // Rule-SHARE-112
    @Test
    public void theReviewListsNoScreenshotRow() {
        try {
            final Path root = Files.createTempDirectory("testin-git-screenshots");
            markATestRun(root);

            assertTrue(PendingChangeReader.toDiffs(List.of(" D " + REMOVED), root, RealJsonCodec.build(), _ -> Map.of(), _ -> Optional.empty()).isEmpty(),
                    "a screenshot arrives or goes with its test run, so it has no row of its own");
        } catch (final Exception ex) {
            throw new AssertionError("the review could not be built", ex);
        }
    }

    @Test
    public void aPictureOutsideATestRunIsAFileLikeAnyOther() {
        try {
            final Path root = Files.createTempDirectory("testin-git-screenshots");
            final String beside = "cases/ts2/k3f9a.png";
            Files.createDirectories(root.resolve("cases/ts2"));
            Files.writeString(root.resolve(beside), "not really a picture");

            assertEquals(PendingChangeReader.toDiffs(List.of("?? " + beside), root, RealJsonCodec.build(), _ -> Map.of(), _ -> Optional.empty()).size(), 1,
                    "a PNG in a test set has no test run to travel with, so the review has to list it");
        } catch (final Exception ex) {
            throw new AssertionError("the review could not be built", ex);
        }
    }

    // Rule-SHARE-112
    @Test
    public void committingATestRunCarriesTheScreenshotsItsFolderGainedOrLost() {
        final List<String> status = List.of(" M " + RESULT, "?? " + ADDED, " D " + REMOVED, "?? " + OTHER_RUN);

        assertEquals(GitCommitAndPush.screenshotsAlongside(status, Set.of(RESULT)), Set.of(ADDED, REMOVED),
                "the screenshots a committed result named, or stopped naming, go with it");
    }

    @Test
    public void committingNoTestRunCarriesNoScreenshot() {
        assertTrue(GitCommitAndPush.screenshotsAlongside(List.of("?? " + ADDED), Set.of("cases/ts2/4fd2a19b.testcase")).isEmpty());
    }
}
