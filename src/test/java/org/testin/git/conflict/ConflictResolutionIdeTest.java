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

package org.testin.git.conflict;

import com.intellij.notification.Notification;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.TitlePanel;
import com.intellij.ui.popup.AbstractPopup;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.git.GitRepositoryService;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class ConflictResolutionIdeTest extends AbstractGitRemoteIdeTest {
    private static final @NotNull String TEST_CASE = "Test Cases/Login/login.testcase";

    private static final @NotNull String MARKER = "Test Runs/Cycle-1/.testrun";

    private static final @NotNull String RUN_ITEM = "Test Runs/Cycle-1/3f1c2b7a-5d4e-4c3b-9a8f-1e2d3c4b5a69.runitem";

    private static final @NotNull String NOTES = "notes.txt";

    private static final @NotNull String BOTH_EXECUTED = """
            { "status" : "BLOCKED", "executedBy" : "Omar", "executedAt" : "Monday 14-09-2026 At 09:00:00 [Asia/Riyadh]" }""";

    private static final @NotNull String MINE_EXECUTED = """
            { "status" : "PASSED", "executedBy" : "Muteb", "executedAt" : "Monday 14-09-2026 At 10:00:00 [Asia/Riyadh]" }""";

    private static final @NotNull String THEIRS_EXECUTED = """
            { "status" : "FAILED", "executedBy" : "Sara", "executedAt" : "Monday 14-09-2026 At 11:30:00 [Asia/Riyadh]" }""";

    private final @NotNull List<String> finished = new CopyOnWriteArrayList<>();

    private final @NotNull List<List<String>> leftOver = new CopyOnWriteArrayList<>();

    private static @NotNull String aTestCase(final @NotNull String description, final @NotNull String expected, final @NotNull String updatedBy, final @NotNull String updatedAt) {
        return "{\"description\":\"" + description + "\",\"expectedResult\":\"" + expected + "\",\"updatedBy\":\"" + updatedBy + "\",\"updatedAt\":\"" + updatedAt + "\"}";
    }

    private static @NotNull String aTestCase(final @NotNull String description) {
        return aTestCase(description, "The dashboard opens", "Muteb", "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]");
    }

    private static @NotNull String runItem(final @NotNull String... executions) {
        return "{ \"default\" : " + executions.length + ", \"executions\" : [ " + String.join(", ", executions) + " ] }";
    }

    private static @NotNull String marker(final @NotNull String status, final @NotNull String updatedBy) {
        return "{ \"createdBy\" : \"Muteb\", \"createdAt\" : \"Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]\", \"updatedBy\" : \"" + updatedBy + "\", \"status\" : \"" + status + "\" }";
    }

    private static void removeTheTestCase(final @NotNull Path repository) {
        try {
            Files.delete(repository.resolve(TEST_CASE));
        } catch (final IOException ex) {
            throw new AssertionError("Could not remove " + TEST_CASE + ": " + ex.getMessage(), ex);
        }
    }

    private @NotNull List<String> pulledIntoAConflict(final @NotNull Consumer<Path> base, final @NotNull Consumer<Path> theirs, final @NotNull Consumer<Path> mine) {
        base.accept(work);
        commitAll(work, "base");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull Path colleague = colleague();
        theirs.accept(colleague);
        commitAll(colleague, "theirs");
        mustGit(colleague, "push", "origin", MAIN);

        mine.accept(work);
        commitAll(work, "mine");
        assertTrue("the pull did not stop on a conflict", git(work, "pull", "--rebase", "origin", MAIN).isEmpty());
        return new GitRepositoryService(getProject()).conflictingPaths(work);
    }

    private void resolving(final @NotNull List<String> conflicting) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> ConflictResolution.resolve(getProject(), work, conflicting, () -> finished.add("resolved"), leftOver::add));
    }

    private void ended() {
        Await.until("the conflict was neither resolved nor left over", () -> !finished.isEmpty() || !leftOver.isEmpty());
    }

    private @NotNull List<String> stillConflicting() {
        return new GitRepositoryService(getProject()).conflictingPaths(work);
    }

    private @NotNull List<String> bothChangedTheDescription() {
        return pulledIntoAConflict(
                repository -> write(repository, TEST_CASE, aTestCase("Log in")),
                repository -> write(repository, TEST_CASE, aTestCase("Sign in")),
                repository -> write(repository, TEST_CASE, aTestCase("Log in with a password")));
    }

    // UC-SHARE-017, Rule-SHARE-078, Rule-SHARE-109
    public void testATestCaseEachSideChangedInAnotherFieldIsMergedAndStagedAndTheSettledAuditIsSaid() {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        final @NotNull List<String> conflicting = pulledIntoAConflict(
                repository -> write(repository, TEST_CASE, aTestCase("Log in")),
                repository -> write(repository, TEST_CASE, aTestCase("Sign in", "The dashboard opens", "Sara", "Monday 14-09-2026 At 11:40:00 [Asia/Riyadh]")),
                repository -> write(repository, TEST_CASE, aTestCase("Log in", "The account page opens", "Muteb", "Monday 14-09-2026 At 10:05:00 [Asia/Riyadh]")));

        resolving(conflicting);
        ended();

        assertEquals("a test case both sides changed in other fields was not resolved", List.of("resolved"), finished);
        final @NotNull String merged = read(work, TEST_CASE);
        assertTrue("the merge lost a side's change: " + merged, merged.contains("Sign in") && merged.contains("The account page opens"));
        assertTrue("who changed it last is not the later edit: " + merged, merged.contains("Sara"));
        assertEquals("the merged test case was not staged", List.of(), stillConflicting());
        assertEquals("the settled audit was not said", Bundle.message("git.merge.settled.title"), titled(said, Bundle.message("git.merge.settled.title")).getTitle());
    }

    // UC-SHARE-018, Rule-SHARE-081, Rule-SHARE-082
    public void testAFieldBothSidesChangedIsAskedAboutUnderTheTestCasesNameAndTheTestersOwnValueIsKept() {
        resolving(bothChangedTheDescription());

        ShownDialog.waitedFor(getProject(), ResolveConflictDialog.class);
        final @NotNull JBPopup question = ShownDialog.popup(getProject(), ResolveConflictDialog.class);
        assertEquals("the question is not titled with the test case's own name", Bundle.message("dialog.conflict.title", "Log in with a password"), ((TitlePanel) ((AbstractPopup) question).getTitle()).getLabel().getText());
        ShownDialog.press(getProject(), ResolveConflictDialog.class, Shortcuts.Enter);
        ended();

        assertEquals("answering the question did not resolve the test case", List.of("resolved"), finished);
        assertTrue("the tester's own value was not kept: " + read(work, TEST_CASE), read(work, TEST_CASE).contains("Log in with a password"));
        assertEquals("the answered test case was not staged", List.of(), stillConflicting());
    }

    // UC-SHARE-018, Rule-SHARE-084
    public void testEscapeOnTheQuestionLeavesTheTestCaseOverAndNamesIt() {
        resolving(bothChangedTheDescription());

        ShownDialog.waitedFor(getProject(), ResolveConflictDialog.class);
        ShownDialog.press(getProject(), ResolveConflictDialog.class, Shortcuts.Escape);
        ended();

        assertEquals("a skipped test case was not named as left over", List.of(List.of(TEST_CASE)), leftOver);
        assertEquals("a skipped test case was staged", List.of(TEST_CASE), stillConflicting());
    }

    // UC-SHARE-017, Rule-SHARE-080, Rule-SHARE-081
    public void testATestRunsMarkerAndRunItemMergeWithoutAQuestion() {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        final @NotNull List<String> conflicting = pulledIntoAConflict(
                repository -> {
                    write(repository, MARKER, marker("CREATED", "Muteb"));
                    write(repository, RUN_ITEM, runItem(BOTH_EXECUTED));
                },
                repository -> {
                    write(repository, MARKER, marker("COMPLETED", "Sara"));
                    write(repository, RUN_ITEM, runItem(BOTH_EXECUTED, THEIRS_EXECUTED));
                },
                repository -> {
                    write(repository, MARKER, marker("IN_PROGRESS", "Muteb"));
                    write(repository, RUN_ITEM, runItem(BOTH_EXECUTED, MINE_EXECUTED));
                });
        assertEquals("the pull did not stop on both of the test run's files", Set.of(MARKER, RUN_ITEM), Set.copyOf(conflicting));

        resolving(conflicting);
        ended();

        assertEquals("a test run's files waited for an answer", List.of("resolved"), finished);
        assertTrue("the test run's status is not the one further along: " + read(work, MARKER), read(work, MARKER).contains("COMPLETED"));
        assertTrue("the run item lost an execution: " + read(work, RUN_ITEM), read(work, RUN_ITEM).contains("Muteb") && read(work, RUN_ITEM).contains("Sara") && read(work, RUN_ITEM).contains("Omar"));
        assertEquals("the merged files were not staged", List.of(), stillConflicting());
        assertEquals("the choice made for the run item was not said", Bundle.message("git.merge.settled.title"), titled(said, Bundle.message("git.merge.settled.title")).getTitle());
    }

    // UC-SHARE-017, Rule-SHARE-079
    public void testATestCaseOneSideRemovedAndAFileNoMergerReadsAreLeftOver() {
        final @NotNull List<String> conflicting = pulledIntoAConflict(
                repository -> {
                    write(repository, TEST_CASE, aTestCase("Log in"));
                    write(repository, NOTES, "first notes");
                },
                repository -> {
                    write(repository, TEST_CASE, aTestCase("Sign in"));
                    write(repository, NOTES, "their notes");
                },
                repository -> {
                    removeTheTestCase(repository);
                    write(repository, NOTES, "my notes");
                });
        assertEquals("the pull did not stop on both files", Set.of(TEST_CASE, NOTES), Set.copyOf(conflicting));

        resolving(conflicting);
        ended();

        assertTrue("a file Testin cannot merge was reported resolved", finished.isEmpty());
        assertEquals("the files Testin cannot merge were not named", List.of(Set.of(TEST_CASE, NOTES)), leftOver.stream().map(Set::copyOf).toList());
    }
}
