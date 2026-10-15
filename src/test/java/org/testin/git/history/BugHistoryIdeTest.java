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

package org.testin.git.history;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.util.JsonCodec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.testin.git.LocalGit.mustGit;

public class BugHistoryIdeTest extends AbstractGitRemoteIdeTest {

    private static final @NotNull UUID TEST_CASE_UUID = UUID.fromString("77777777-7777-4777-8777-777777777397");

    private static final @NotNull String RUN_ITEM = TEST_CASE_UUID + ".runitem";

    private static @NotNull RunItem runItem(final @NotNull RunItemStatus runItemStatus, final @NotNull BugSeverity severity) {
        return RunItem.builder().uuid(TEST_CASE_UUID).executions(List.of(RunItemExecution.builder().status(runItemStatus).bugSeverity(severity).actualResult("The basket was emptied").executedBy("Sara").build())).build();
    }

    private static @NotNull List<String> told(final @NotNull List<BugCard> bugs) {
        return bugs.stream().map(bug -> (bug.isCommitted() ? "" : "not committed ") + bug.event().kind() + " in " + bug.event().testRunName()).toList();
    }

    private void save(final @NotNull String testRun, final @NotNull RunItem runItem) {
        write(work, "Test Runs/" + testRun + "/" + RUN_ITEM, Services.getInstance(getProject(), JsonCodec.class).writeValueAsString(runItem));
    }

    private void holdsNow(final @NotNull Path testProjectPath, final @NotNull String testRun, final @NotNull RunItem runItem) {
        Services.getInstance(getProject(), TestRuns.class).putRunItems(testProjectPath.resolve("Test Runs").resolve(testRun), RunItems.builder().all(new ArrayList<>(List.of(runItem))).build());
    }

    private @NotNull List<BugCard> bugsOf(final @NotNull Path testProjectPath) {
        final @NotNull Map<Path, String> now = BugHistory.runItemsNow(getProject(), TEST_CASE_UUID);
        try {
            return ApplicationManager.getApplication().executeOnPooledThread(() -> BugHistory.addTo(History.read(List.of()), getProject(), new TestCaseFile(testProjectPath, Path.of("Test Cases", "Login", TEST_CASE_UUID + ".testcase")), TEST_CASE_UUID, now).bugs()).get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new AssertionError("the bugs were never read", ex);
        }
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-106, Rule-VIEW-PANEL-107, Rule-VIEW-PANEL-108
    public void testEveryBugGitRecordedIsABugCardNewestFirstWithTheOneNotCommittedOnTop() {
        save("Cycle 3", runItem(RunItemStatus.FAILED, BugSeverity.MINOR));
        save("Cycle 4", runItem(RunItemStatus.FAILED, BugSeverity.MAJOR));
        save("Cycle 5", runItem(RunItemStatus.PASSED, BugSeverity.MINOR));
        commitAll(work, "Cycle 3 results");
        save("Cycle 3", runItem(RunItemStatus.FAILED, BugSeverity.MAJOR));
        save("Cycle 5", RunItem.builder().uuid(TEST_CASE_UUID).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).executedBy("Muteb").build())).build());
        commitAll(work, "Severity");
        save("Cycle 3", runItem(RunItemStatus.PASSED, BugSeverity.MAJOR));
        mustGit(work, "rm", "-q", "Test Runs/Cycle 4/" + RUN_ITEM);
        commitAll(work, "Re-run");

        mustGit(work, "checkout", "-q", "-b", "side");
        save("Cycle 5", runItem(RunItemStatus.FAILED, BugSeverity.MAJOR));
        commitAll(work, "Side branch");
        mustGit(work, "checkout", "-q", MAIN);

        holdsNow(work, "Cycle 3", runItem(RunItemStatus.FAILED, BugSeverity.MAJOR));
        holdsNow(work, "Cycle 5", RunItem.builder().uuid(TEST_CASE_UUID).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).executedBy("Muteb").build())).build());

        final @NotNull List<BugCard> bugs = bugsOf(work);

        assertEquals(List.of("not committed RECORDED in Cycle 3", "CLEARED in Cycle 3", "REMOVED in Cycle 4", "CHANGED in Cycle 3", "RECORDED in Cycle 3", "RECORDED in Cycle 4"), told(bugs));
        assertEquals(head(work, "HEAD"), bugs.get(1).hash());
        assertEquals(BugSeverity.MAJOR.getLabel(), bugs.get(3).event().changes().getFirst().newValue());
        assertEquals(work.resolve("Test Runs").resolve("Cycle 3"), bugs.get(1).event().testRun());
    }

    // Rule-VIEW-PANEL-100
    public void testAFolderNotUnderGitShowsTheBugsItsTestRunsHoldNow() {
        final @NotNull Path alone = directory("alone");
        holdsNow(alone, "Cycle 1", runItem(RunItemStatus.FAILED, BugSeverity.MAJOR));
        holdsNow(alone, "Cycle 2", runItem(RunItemStatus.PASSED, BugSeverity.MAJOR));

        assertEquals(List.of("not committed RECORDED in Cycle 1"), told(bugsOf(alone)));
    }
}
