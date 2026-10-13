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

import com.intellij.notification.Notification;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.git.ShareGestures;
import org.testin.git.review.PendingChangesDialog;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.ChangedSinceCommit;
import org.testin.testrun.RunItemRecorder;
import org.testin.testrun.TestRunStatusChange;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JTable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class CommittedTestRunIdeTest extends AbstractReadTheRootIdeTest {
    private static final @NotNull String DESCRIPTION = "Log in with a valid user and reach the dashboard";
    private static final @NotNull String COMPLETED = "Cycle 4";
    private static final @NotNull String MESSAGE = "Step 4 reads Press Pay";

    private Path testProjectPath;
    private Path testSet;
    private UUID testCaseId;

    private static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String aTestRunMarker(final @NotNull TestRunStatus testRunStatus) {
        return """
                {
                  "createdBy" : "Sara",
                  "createdAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
                  "updatedBy" : "Sara",
                  "updatedAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
                  "status" : "%s"
                }""".formatted(testRunStatus.name());
    }

    @Override
    protected void setUp() {
        super.setUp();
        testProjectPath = aTestProjectAt(root.resolve("Shop"));
        testSet = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        testCaseId = aTestCaseIn(testSet);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), PendingChangesDialog.class);
        super.tearDown();
    }

    private @NotNull Path aTestRun(final @NotNull String name, final @NotNull TestRunStatus testRunStatus) {
        final @NotNull Path testRun = theTestRunsOf(testProjectPath).resolve(name);
        write(testRun.resolve(NodeType.TEST_RUN.getMarker()), aTestRunMarker(testRunStatus));
        resultIn(testRun, FileKind.RUN_ITEM.fileName(testCaseId), testCaseId);
        return testRun;
    }

    private void committedAsItIs() {
        mustGit(testProjectPath, "init", "--initial-branch=main");
        mustGit(testProjectPath, "config", "user.name", "Sara");
        mustGit(testProjectPath, "config", "user.email", "sara@example.invalid");
        mustGit(testProjectPath, "add", "-A");
        mustGit(testProjectPath, "commit", "-q", "-m", "base");
    }

    private void theTestCaseReads(final @NotNull String description) {
        theTestCaseReads(testCaseId, description);
    }

    private void theTestCaseReads(final @NotNull UUID testCaseId, final @NotNull String description) {
        final @NotNull Path file = testSet.resolve(FileKind.TEST_CASE.fileName(testCaseId));
        write(file, read(file).replaceFirst("\"description\" : \"[^\"]*\"", "\"description\" : \"" + description + "\""));
    }

    private @NotNull String subject(final @NotNull String revision) {
        return mustGit(testProjectPath, "log", "-1", "--format=%s", revision).trim();
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull TestRunNode theTestRunAt(final @NotNull Path testRun) {
        return indexedTestRuns().findTestRunNode(testRun).orElseThrow(() -> new AssertionError(testRun.getFileName() + " is not indexed"));
    }

    private @NotNull RunItem theRunItemOf(final @NotNull Path testRun) {
        return theRunItemOf(testRun, testCaseId);
    }

    private @NotNull RunItem theRunItemOf(final @NotNull Path testRun, final @NotNull UUID testCaseId) {
        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> TestRunFromGit.read(getProject(), testRun)).get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new AssertionError("the commit of " + testRun.getFileName() + " was never read", ex);
        }
        final @NotNull RunItems held = indexedTestRuns().getRunItems(testRun);
        return held.runItemOf(testCaseId).orElseThrow(() -> new AssertionError(testRun.getFileName() + " does not cover the test case"));
    }

    private void reviewed(final @NotNull String message, final @NotNull Consumer<JComponent> button) {
        final @NotNull JComponent review = ShareGestures.theReviewOf(getProject(), testProjectPath);
        ShareGestures.type(review, message);
        button.accept(review);
    }

    private boolean isTheSubjectOf(final @NotNull String revision, final @NotNull String message) {
        return git(testProjectPath, "log", "-1", "--format=%s", revision).map(String::trim).filter(message::equals).isPresent();
    }

    private void recordedAfter(final @NotNull Consumer<JComponent> button) {
        theTestCaseReads("Press Pay");
        readEverything();
        reviewed(MESSAGE, button);
        Await.until("no commit followed the tester's", () -> isTheSubjectOf("HEAD^", MESSAGE));
    }

    private void committedWithTheReview() {
        recordedAfter(ShareGestures::commitOnly);
    }

    private @NotNull Path aRemote() {
        final @NotNull Path remote = root.resolve("remote.git");
        try {
            Files.createDirectories(remote);
        } catch (final IOException ex) {
            throw new AssertionError("Could not make the remote: " + ex.getMessage(), ex);
        }
        mustGit(remote, "init", "--bare", "--initial-branch=main");
        mustGit(testProjectPath, "remote", "add", "origin", remote.toUri().toString());
        mustGit(testProjectPath, "push", "-q", "-u", "origin", "main");
        return remote;
    }

    // UC-SHARE-012, Rule-SHARE-130
    public void testACommitMakesTheCompletedTestRunCommittedAndCommitsItsMarkerStraightAfter() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        final @NotNull Path inProgress = aTestRun("Cycle 5", TestRunStatus.IN_PROGRESS);
        committedAsItIs();

        committedWithTheReview();

        final @NotNull String testersCommit = mustGit(testProjectPath, "rev-parse", "HEAD^").trim();
        assertEquals(Bundle.message("test.run.record.commit", COMPLETED), subject("HEAD"));
        assertEquals(MESSAGE, subject("HEAD^"));
        assertEquals(TestRunStatus.COMMITTED, theTestRunAt(completed).getMarker().getStatus());
        assertEquals(testersCommit, theTestRunAt(completed).getMarker().getCommit());
        assertTrue("the commit was not written into the .testrun", read(completed.resolve(NodeType.TEST_RUN.getMarker())).contains(testersCommit));
        assertEquals("the .testrun was left out of Testin's commit", "", mustGit(testProjectPath, "status", "--porcelain").trim());
        assertEquals("a test run that was not Completed changed", TestRunStatus.IN_PROGRESS, theTestRunAt(inProgress).getMarker().getStatus());
        assertEquals("", theTestRunAt(inProgress).getMarker().getCommit());
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239, Rule-EDITOR-PANEL-263
    public void testACommittedTestRunShowsTheTestCaseFromItsCommitAndANotCommittedOneShowsItNow() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        final @NotNull Path inProgress = aTestRun("Cycle 5", TestRunStatus.IN_PROGRESS);
        committedAsItIs();
        committedWithTheReview();

        theTestCaseReads("Press Pay now");
        readEverything();

        final @NotNull RunItem committed = theRunItemOf(completed);
        assertEquals("Press Pay", committed.shownTestCase().getDescription());
        assertTrue("the change since the commit is not said", ChangedSinceCommit.of(committed));

        final @NotNull RunItem open = theRunItemOf(inProgress);
        assertEquals("Press Pay now", open.shownTestCase().getDescription());
        assertFalse(ChangedSinceCommit.of(open));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239
    public void testATestCaseDeletedAfterTheCommitKeepsItsTextAndItsRunItemStatus() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        committedWithTheReview();

        try {
            Files.delete(testSet.resolve(FileKind.TEST_CASE.fileName(testCaseId)));
        } catch (final IOException ex) {
            throw new AssertionError("Could not delete the test case: " + ex.getMessage(), ex);
        }
        readEverything();

        final @NotNull RunItem committed = theRunItemOf(completed);
        assertFalse("a committed test run lost a run item", committed.isRemoved());
        assertEquals("a Committed test run changed the run item status its commit recorded", RunItemStatus.PASSED, committed.getStatus());
        assertEquals("Press Pay", committed.shownTestCase().getDescription());
    }

    private void theTestCaseIsDeleted() {
        try {
            Files.delete(testSet.resolve(FileKind.TEST_CASE.fileName(testCaseId)));
        } catch (final IOException ex) {
            throw new AssertionError("Could not delete the test case: " + ex.getMessage(), ex);
        }
        readEverything();
    }

    private @NotNull List<TestCaseHistoryCard> theHistoryOfTheDeletedTestCase() {
        Services.getInstance(getProject(), BoundTestProject.class).choose(String.valueOf(testProjectPath.getFileName()));
        try {
            return ApplicationManager.getApplication().executeOnPooledThread(() -> TestCaseHistory.deletedFile(getProject(), testCaseId)
                    .map(file -> TestCaseHistory.read(getProject(), file, Optional.empty()).entries())
                    .orElseThrow(() -> new AssertionError("Git was not asked where the deleted test case was"))).get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new AssertionError("the history was never read", ex);
        }
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    public void testADeletedTestCaseInATestRunNotCommittedReadsRemovedWithItsLastTextInGit() {
        final @NotNull Path inProgress = aTestRun(COMPLETED, TestRunStatus.IN_PROGRESS);
        committedAsItIs();
        theTestCaseIsDeleted();

        final @NotNull RunItem runItem = theRunItemOf(inProgress);
        assertEquals("a deleted test case was not Removed", RunItemStatus.REMOVED, runItem.getStatus());
        assertEquals("the run item did not show the last text Git holds", DESCRIPTION, runItem.shownTestCase().getDescription());
        assertTrue("a deleted test case took a run item status", Services.getInstance(getProject(), RunItemRecorder.class).heldTestRun(inProgress).flatMap(testRun -> testRun.runItemOf(testCaseId)).filter(RunItem::isRemoved).isPresent());
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-113
    public void testTheHistoryOfATestCaseDeletedAndNotCommittedEndsWithARemovedCardNotCommittedYet() {
        committedAsItIs();
        theTestCaseIsDeleted();

        final @NotNull List<TestCaseHistoryCard> entries = theHistoryOfTheDeletedTestCase();
        assertEquals(List.of(TestCaseHistoryKind.REMOVED, TestCaseHistoryKind.CREATED), entries.stream().map(TestCaseHistoryCard::kind).toList());
        assertFalse("the deletion is not committed yet", entries.getFirst().isCommitted());
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-113
    public void testTheHistoryOfATestCaseWhoseDeletionIsCommittedEndsWithTheCommitThatRemovedIt() {
        committedAsItIs();
        theTestCaseIsDeleted();
        mustGit(testProjectPath, "commit", "-q", "-am", "Remove the login test case");

        final @NotNull List<TestCaseHistoryCard> entries = theHistoryOfTheDeletedTestCase();
        assertEquals(List.of(TestCaseHistoryKind.REMOVED, TestCaseHistoryKind.CREATED), entries.stream().map(TestCaseHistoryCard::kind).toList());
        assertEquals("Remove the login test case", entries.getFirst().message());
    }

    // Rule-TREE-PANEL-009, Rule-PRODUCT-011
    public void testOnlyACommittedTestRunRefusesARunItemStatus() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        final @NotNull Path closed = aTestRun("Cycle 5", TestRunStatus.CLOSED);
        final @NotNull Path committed = aTestRun("Cycle 6", TestRunStatus.COMMITTED);
        readEverything();
        final @NotNull RunItemRecorder runItemStatuses = Services.getInstance(getProject(), RunItemRecorder.class);

        assertTrue("a Completed test run refused a run item status", runItemStatuses.heldTestRun(completed).isPresent());
        assertTrue("a Closed test run refused a run item status", runItemStatuses.heldTestRun(closed).isPresent());
        assertTrue("a Committed test run took a run item status", runItemStatuses.heldTestRun(committed).isEmpty());
        assertEquals(DESCRIPTION, theRunItemOf(completed).shownTestCase().getDescription());
    }

    // UC-SHARE-013, Rule-SHARE-130
    public void testCommitAndPushSendsBothCommitsInOnePush() {
        aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        final @NotNull Path remote = aRemote();

        recordedAfter(ShareGestures::commitAndPush);

        final @NotNull String recorded = mustGit(testProjectPath, "rev-parse", "HEAD").trim();
        Await.until("the two commits never reached the remote", () -> git(remote, "rev-parse", "main").map(String::trim).filter(recorded::equals).isPresent());
        assertEquals(MESSAGE, mustGit(remote, "log", "-1", "--format=%s", "main^").trim());
    }

    // UC-SHARE-012, Rule-SHARE-130, Rule-TREE-PANEL-009
    public void testEveryCompletedTestRunIsRecordedInOneCommitAndAClosedOneIsLeftAsItIs() {
        final @NotNull Path first = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        final @NotNull Path second = aTestRun("Cycle 7", TestRunStatus.COMPLETED);
        final @NotNull Path closed = aTestRun("Cycle 5", TestRunStatus.CLOSED);
        committedAsItIs();

        committedWithTheReview();

        final @NotNull String testersCommit = mustGit(testProjectPath, "rev-parse", "HEAD^").trim();
        assertEquals(testersCommit, theTestRunAt(first).getMarker().getCommit());
        assertEquals(testersCommit, theTestRunAt(second).getMarker().getCommit());
        assertTrue("one commit does not name both test runs: " + subject("HEAD"), subject("HEAD").contains(COMPLETED) && subject("HEAD").contains("Cycle 7"));
        assertEquals("a Closed test run became a record", TestRunStatus.CLOSED, theTestRunAt(closed).getMarker().getStatus());
        assertEquals("", theTestRunAt(closed).getMarker().getCommit());
    }

    // Rule-TREE-PANEL-135, Rule-SHARE-130
    public void testACommittedTestRunKeepsItsCommitWhenTheNextCommitComes() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        committedWithTheReview();
        final @NotNull String recorded = theTestRunAt(completed).getMarker().getCommit();

        theTestCaseReads("Press Pay now");
        readEverything();
        reviewed("Step 4 reads Press Pay now", ShareGestures::commitOnly);
        Await.until("the second commit was never made", () -> isTheSubjectOf("HEAD", "Step 4 reads Press Pay now"));
        indexedTestRuns().awaitWrites();

        assertEquals("a Committed test run took a second commit", recorded, theTestRunAt(completed).getMarker().getCommit());
        assertEquals(TestRunStatus.COMMITTED, theTestRunAt(completed).getMarker().getStatus());
        assertEquals("Testin recorded a test run again", "Step 4 reads Press Pay now", subject("HEAD"));
        assertEquals("Press Pay", theRunItemOf(completed).shownTestCase().getDescription());
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239
    public void testATestCaseMovedToAnotherTestSetAfterTheCommitIsStillReadFromTheCommit() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        committedWithTheReview();

        final @NotNull Path checkout = marked(theTestCasesOf(testProjectPath).resolve("Checkout"), NodeType.TEST_SET);
        final @NotNull Path moved = checkout.resolve(FileKind.TEST_CASE.fileName(testCaseId));
        try {
            Files.move(testSet.resolve(FileKind.TEST_CASE.fileName(testCaseId)), moved);
        } catch (final IOException ex) {
            throw new AssertionError("Could not move the test case: " + ex.getMessage(), ex);
        }
        write(moved, read(moved).replace("Press Pay", "Press Pay now"));
        readEverything();

        final @NotNull RunItem committed = theRunItemOf(completed);
        assertEquals("Press Pay", committed.shownTestCase().getDescription());
        assertTrue(ChangedSinceCommit.of(committed));
    }

    // Rule-SHARE-130
    public void testACommitMadeOutsideTheReviewMakesNoTestRunCommitted() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        theTestCaseReads("Press Pay");
        mustGit(testProjectPath, "commit", "-q", "-am", "made by hand");
        readEverything();

        assertEquals("a commit Testin did not make recorded a test run", TestRunStatus.COMPLETED, theTestRunAt(completed).getMarker().getStatus());
        assertEquals("", theTestRunAt(completed).getMarker().getCommit());
        assertEquals("Press Pay", theRunItemOf(completed).shownTestCase().getDescription());
    }

    private @NotNull Path aCommittedTestRunFrom(final @NotNull String commit) {
        final @NotNull Path testRun = aTestRun(COMPLETED, TestRunStatus.COMMITTED);
        final @NotNull Path marker = testRun.resolve(NodeType.TEST_RUN.getMarker());
        write(marker, read(marker).replace("\"status\" : \"COMMITTED\"", "\"status\" : \"COMMITTED\",\n  \"commit\" : \"" + commit + "\""));
        return testRun;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239
    public void testACommitThisMachineDoesNotHaveShowsTheTestCaseAsItIsNow() {
        final @NotNull Path committed = aCommittedTestRunFrom("0123456789abcdef0123456789abcdef01234567");
        committedAsItIs();
        theTestCaseReads("Press Pay");
        readEverything();

        final @NotNull RunItem runItem = theRunItemOf(committed);
        assertEquals("Press Pay", runItem.shownTestCase().getDescription());
        assertFalse(ChangedSinceCommit.of(runItem));
        assertTrue("a Committed test run took a run item status", Services.getInstance(getProject(), RunItemRecorder.class).heldTestRun(committed).isEmpty());
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239
    public void testATestProjectNotUnderGitShowsTheTestCaseAsItIsNow() {
        final @NotNull Path committed = aCommittedTestRunFrom("ea9a50107afbbaa1831909436b781f0e3c2d1a55");
        theTestCaseReads("Press Pay");
        readEverything();

        assertEquals("Press Pay", theRunItemOf(committed).shownTestCase().getDescription());
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239
    public void testEveryTestRunRecordedInOneCommitShowsItsOwnTestCasesFromIt() {
        final @NotNull Path first = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        final @NotNull UUID other = aTestCaseIn(testSet);
        final @NotNull Path second = theTestRunsOf(testProjectPath).resolve("Cycle 5");
        write(second.resolve(NodeType.TEST_RUN.getMarker()), aTestRunMarker(TestRunStatus.COMPLETED));
        resultIn(second, FileKind.RUN_ITEM.fileName(other), other);
        committedAsItIs();
        committedWithTheReview();

        theTestCaseReads("Edited after the commit");
        theTestCaseReads(other, "Edited after the commit");
        readEverything();

        assertEquals("Press Pay", theRunItemOf(first).shownTestCase().getDescription());
        assertEquals("the second test run of the commit shows its test case as it is now", DESCRIPTION, theRunItemOf(second, other).shownTestCase().getDescription());
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239, Rule-SHARE-130
    public void testATestRunRecordedBeforeARebaseIsReadFromTheCommitTheRebaseMade() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        final @NotNull Path remote = aRemote();
        final @NotNull Path teammate = root.resolve("teammate");
        mustGit(root, "clone", "-q", remote.toUri().toString(), teammate.toString());
        mustGit(teammate, "config", "user.name", "Omar");
        mustGit(teammate, "config", "user.email", "omar@example.invalid");
        write(teammate.resolve("README.md"), "A teammate's change");
        mustGit(teammate, "add", "-A");
        mustGit(teammate, "commit", "-q", "-m", "teammate");
        mustGit(teammate, "push", "-q", "origin", "main");
        committedWithTheReview();

        final @NotNull String recorded = theTestRunAt(completed).getMarker().getCommit();
        mustGit(testProjectPath, "pull", "-q", "--rebase", "origin", "main");
        git(testProjectPath, "update-ref", "-d", "ORIG_HEAD");
        mustGit(testProjectPath, "reflog", "expire", "--expire=now", "--all");
        mustGit(testProjectPath, "gc", "-q", "--prune=now");
        assertTrue("the rebase kept the recorded commit, so this test proves nothing", git(testProjectPath, "cat-file", "-e", recorded + "^{commit}").isEmpty());
        theTestCaseReads("Edited after the commit");
        readEverything();

        assertEquals("the test run is read as it is now once its commit was rewritten", "Press Pay", theRunItemOf(completed).shownTestCase().getDescription());
    }

    // UC-SHARE-012, Rule-SHARE-127, Rule-SHARE-130
    public void testARecordCommitThatFailsSaysTheTestersCommitIsIn() {
        aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        write(testProjectPath.resolve(".git/hooks/commit-msg"), """
                #!/bin/sh
                grep -q 'Record test run' "$1" && exit 1
                exit 0
                """);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        theTestCaseReads("Press Pay");
        readEverything();
        reviewed(MESSAGE, ShareGestures::commitOnly);

        Await.until("a record commit that failed was not reported on its own", () -> said.stream().anyMatch(notification -> notification.getTitle().contains(Bundle.message("git.record.failed.title"))));
        assertEquals("the tester's commit did not land", MESSAGE, subject("HEAD"));
        assertTrue("the record failure was reported as the tester's commit failing", said.stream().noneMatch(notification -> notification.getTitle().contains(Bundle.message("git.commit.failed.title"))));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    public void testACommittedTestRunWhoseCommitIsNotReadYetKeepsTheRunItemStatusOfADeletedTestCase() {
        final @NotNull Path committed = aCommittedTestRunFrom("0123456789abcdef0123456789abcdef01234567");
        committedAsItIs();
        theTestCaseIsDeleted();

        final @NotNull RunItem runItem = indexedTestRuns().getRunItems(committed).runItemOf(testCaseId).orElseThrow();
        assertFalse("a Committed test run reads Removed for a test case it has not read from its commit yet", runItem.isRemoved());
        assertEquals(RunItemStatus.PASSED, runItem.getStatus());
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-136
    public void testCompletingATestRunOffersTheCommit() {
        final @NotNull Path testRun = aTestRun(COMPLETED, TestRunStatus.IN_PROGRESS);
        committedAsItIs();
        readEverything();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        Services.getInstance(getProject(), TestRunStatusChange.class).apply(theTestRunAt(testRun), TestRunStatus.COMPLETED);

        Await.until("completing the test run did not offer the commit", () -> said.stream().anyMatch(notification -> notification.getContent().contains(COMPLETED)
                && notification.getActions().stream().anyMatch(action -> Bundle.message("action.Testin.ViewPendingCommits.text").equals(action.getTemplateText()))));
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-136
    public void testCompletingATestRunNotUnderGitOffersNoCommitButSaysItIsCompleted() {
        final @NotNull Path testRun = aTestRun(COMPLETED, TestRunStatus.IN_PROGRESS);
        readEverything();
        final @NotNull Said listening = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull List<Notification> said = listening.notifications();

        Services.getInstance(getProject(), TestRunStatusChange.class).apply(theTestRunAt(testRun), TestRunStatus.COMPLETED);

        assertTrue("a test project not under Git was offered a commit", said.stream().noneMatch(notification -> notification.getActions().stream().anyMatch(action -> Bundle.message("action.Testin.ViewPendingCommits.text").equals(action.getTemplateText()))));
        Await.until("completing a test run not under Git said nothing", () -> listening.shown().contains(TestRunStatus.COMPLETED.getLabel()));
    }

    // Rule-EDITOR-PANEL-266
    public void testRunIsRefusedOnACommittedTestRunAndOfferedOnACompletedOne() {
        final @NotNull Path committed = aTestRun(COMPLETED, TestRunStatus.COMMITTED);
        final @NotNull Path completed = aTestRun("Cycle 5", TestRunStatus.COMPLETED);
        readEverything();
        final @NotNull TestCase testCase = Services.getInstance(getProject(), TestCases.class).findTestCase(testCaseId).orElseThrow();

        assertTrue("Run was offered on a Committed test run", TestCaseQuickAction.RUN_TEST_METHOD.whyNotHere(Optional.of(theTestRunAt(committed)), testCase).isPresent());
        assertTrue("Run was refused on a Completed test run", TestCaseQuickAction.RUN_TEST_METHOD.whyNotHere(Optional.of(theTestRunAt(completed)), testCase).isEmpty());
        assertTrue("Run was refused outside any test run", TestCaseQuickAction.RUN_TEST_METHOD.whyNotHere(Optional.empty(), testCase).isEmpty());
    }

    // UC-INTERNAL-002, Rule-EDITOR-PANEL-239
    public void testACommittedTestRunShowsItsRecordAsSoonAsTheIndexIsReadWithoutOpeningIt() {
        committedAsItIs();
        final @NotNull String commit = mustGit(testProjectPath, "rev-parse", "HEAD").trim();
        final @NotNull Path committed = aCommittedTestRunFrom(commit);
        theTestCaseReads("Press Pay now");

        readEverything();

        Await.until("the record was not read once the index was", () -> indexedTestRuns().getRunItems(committed).runItemOf(testCaseId)
                .map(runItem -> runItem.shownTestCase().getDescription().equals(DESCRIPTION)).orElse(false));
    }

    // UC-SHARE-010, Rule-SHARE-129
    public void testEveryMarkerStaysTickedInTheReview() {
        final @NotNull Path testRun = aTestRun("Cycle 4", TestRunStatus.IN_PROGRESS);
        committedAsItIs();
        write(testRun.resolve(NodeType.TEST_RUN.getMarker()), aTestRunMarker(TestRunStatus.ASSIGNED));
        theTestCaseReads("Press Pay");
        readEverything();

        final @NotNull JTable changes = ShareGestures.table(ShareGestures.theReviewOf(getProject(), testProjectPath));
        changes.clearSelection();

        assertTrue("the marker was unticked", changes.getSelectedRowCount() > 0);
        assertTrue("the test case could not be unticked", changes.getSelectedRowCount() < changes.getRowCount());
    }

    // UC-SHARE-012, Rule-SHARE-130
    public void testACompletedTestRunWithAChangeLeftOutOfTheCommitStaysCompleted() {
        final @NotNull Path completed = aTestRun(COMPLETED, TestRunStatus.COMPLETED);
        committedAsItIs();
        write(completed.resolve(NodeType.TEST_RUN.getMarker()), aTestRunMarker(TestRunStatus.COMPLETED).replace("\"updatedBy\" : \"Sara\"", "\"updatedBy\" : \"Omar\""));
        theTestCaseReads("Press Pay");
        readEverything();

        final @NotNull JComponent review = ShareGestures.theReviewOf(getProject(), testProjectPath);
        ShareGestures.table(review).clearSelection();
        ShareGestures.type(review, MESSAGE);
        ShareGestures.commitOnly(review);
        Await.until("the tester's commit never landed", () -> isTheSubjectOf("HEAD", MESSAGE));

        assertEquals("a test run whose test case change was left out became the record", TestRunStatus.COMPLETED, theTestRunAt(completed).getMarker().getStatus());
    }

    // Rule-PRODUCT-007
    public void testEditingATestCaseAfterATestRunJudgedItLeavesWhatTheTestRunRecorded() {
        final @NotNull Path inProgress = aTestRun("Cycle 5", TestRunStatus.IN_PROGRESS);
        final @NotNull Path runItemFile = inProgress.resolve(FileKind.RUN_ITEM.fileName(testCaseId));
        readEverything();
        final @NotNull String recorded = read(runItemFile);
        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        final @NotNull TestCase testCase = testCases.findTestCase(testCaseId).orElseThrow();

        assertTrue("the edit was not saved", testCases.putTestCase(testSet, testCase.edit().description("Press Pay now").expectedResult("The receipt opens").build()));
        indexedTestRuns().awaitWrites();
        readEverything();

        final @NotNull RunItem runItem = indexedTestRuns().getRunItems(inProgress).runItemOf(testCaseId).orElseThrow(() -> new AssertionError("the test run lost its run item"));
        assertEquals("editing the test case changed the run item status the test run recorded", RunItemStatus.PASSED, runItem.getStatus());
        assertEquals("editing the test case rewrote the run item", recorded, read(runItemFile));
    }
}
