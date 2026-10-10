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

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.TempTree;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RescanKeepsTheIndexIdeTest extends AbstractTempRootIdeTest {

    private static final int SETS = 60;
    private static final int TEST_CASES_PER_SET = 10;

    private static final int SCAN_TIMEOUT_SECONDS = 120;

    private @NotNull ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    // Rule-INTERNAL-081
    public void testNothingDisappearsWhileTheProjectIsReadAgain() {
        final Path project = SyntheticTree.write(root, SETS, TEST_CASES_PER_SET);
        indexer().scanSingleProject(project);

        final Path set = project.resolve("Test Cases").resolve("set-0");
        final UUID testCase = indexedTestCases().getTestCasesForTestSet(set).getFirst().getId();

        assertTrue("the first pass did not index the project, so there is nothing to watch",
                nodes().nodeExists(set) && indexedTestCases().findTestCase(testCase).isPresent());

        boolean vanished = false;
        final CountDownLatch scanned = new CountDownLatch(1);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                indexer().scanSingleProject(project);
            } finally {
                scanned.countDown();
            }
        });

        while (scanned.getCount() > 0) {
            if (!nodes().nodeExists(set) || indexedTestCases().findTestCase(testCase).isEmpty()) vanished = true;
            Thread.onSpinWait();
        }

        await(scanned);

        assertFalse("the project was emptied out of the index while it was being read again, so every "
                        + "surface asking about it in that window was answered as though it had been deleted",
                vanished);

        assertTrue("and it is there when the pass has finished",
                nodes().nodeExists(set) && indexedTestCases().findTestCase(testCase).isPresent());
    }

    public void testASetDeletedOnDiskIsGoneAfterTheRescan() {
        final Path project = SyntheticTree.write(root, 3, TEST_CASES_PER_SET);
        indexer().scanSingleProject(project);

        final Path deleted = project.resolve("Test Cases").resolve("set-1");
        final Path kept = project.resolve("Test Cases").resolve("set-2");

        final UUID testCaseInDeleted = indexedTestCases().getTestCasesForTestSet(deleted).getFirst().getId();
        final UUID testCaseInKept = indexedTestCases().getTestCasesForTestSet(kept).getFirst().getId();

        assertTrue("could not delete " + deleted, TempTree.delete(deleted));
        indexer().scanSingleProject(project);

        assertFalse("a test set deleted on disk is still in the index after a rescan",
                nodes().nodeExists(deleted));
        assertTrue("its test cases went with it", indexedTestCases().findTestCase(testCaseInDeleted).isEmpty());
        assertTrue("the test cases of a set nobody touched went too", indexedTestCases().findTestCase(testCaseInKept).isPresent());
        assertTrue("and the sets that are still there are still there", nodes().nodeExists(kept));
    }

    private void await(final CountDownLatch latch) {
        try {
            assertTrue("the rescan did not finish", latch.await(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while the rescan ran", interrupted);
        }
    }
}
