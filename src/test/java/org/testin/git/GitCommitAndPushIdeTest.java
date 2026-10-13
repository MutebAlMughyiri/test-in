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

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.git.change.ChangeSubject;
import org.testin.git.change.GitStatusKind;
import org.testin.git.change.PendingChange;
import org.testin.git.conflict.ConflictResolution;
import org.testin.git.conflict.RebaseEnd;
import org.testin.model.TestCase;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class GitCommitAndPushIdeTest extends AbstractGitRemoteIdeTest {
    private static final int WINDOWS_COMMAND_LINE_LIMIT = 32767;

    private static @NotNull PendingChange ticked(final @NotNull String relativePath, final @NotNull GitStatusKind type) {
        return new PendingChange(ChangeSubject.TEST_CASE, "a test case", "login", UUID.randomUUID().toString(), Path.of(relativePath), type, TestCase.builder().build(), List.of());
    }

    private @NotNull GitCommitAndPush commits() {
        return new GitCommitAndPush(getProject());
    }

    private @NotNull List<String> committedPaths() {
        return mustGit(work, "ls-tree", "-r", "--name-only", "HEAD").lines().filter(line -> !line.isBlank()).toList();
    }

    private void pullStopsOnAConflict() {
        try {
            commits().pullWhereTheRemoteHasBranch(work, "origin", remoteUrl(), MAIN);
            fail("the pull was expected to stop on the conflict");
        } catch (final GitFailed expected) {
            assertTrue(new GitRepositoryService(getProject()).unfinished(work).isPresent());
        }
    }

    private void bothChangeTheNotes() {
        write(work, "notes.txt", "base\n");
        commitAll(work, "the common ancestor");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull Path colleague = colleague();
        write(colleague, "notes.txt", "theirs\n");
        commitAll(colleague, "their change");
        mustGit(colleague, "push", "origin", MAIN);

        write(work, "notes.txt", "mine\n");
        commitAll(work, "my change");
    }

    // UC-SHARE-012, Rule-SHARE-054
    public void testTheMarkersAboveACommittedTestCaseAreCommittedWithIt() {
        write(work, ".testproject", "{}");
        write(work, "Test Cases/.testcasesfolder", "{}");
        write(work, "Test Cases/login/.testset", "{}");
        write(work, "Test Runs/.testrunsfolder", "{}");
        write(work, "Test Cases/login/a.testcase", "{}");

        commits().stageAndCommit(work, "one test case", List.of(ticked("Test Cases/login/a.testcase", GitStatusKind.ADDED)));

        final @NotNull List<String> committed = committedPaths();
        assertTrue(committed.toString(), committed.containsAll(List.of(".testproject", "Test Cases/.testcasesfolder", "Test Cases/login/.testset", "Test Cases/login/a.testcase")));
        assertFalse("no test case sits under Test Runs, so its marker waits: " + committed, committed.contains("Test Runs/.testrunsfolder"));
    }

    // UC-SHARE-012, Rule-SHARE-055
    public void testATestCaseThatIsGoneIsCommittedAsARemoval() {
        write(work, "Test Cases/login/a.testcase", "{}");
        commitAll(work, "a test case");
        mustGit(work, "mv", "Test Cases/login/a.testcase", "Test Cases/a.testcase");

        commits().stageAndCommit(work, "moved the test case", List.of(
                ticked("Test Cases/login/a.testcase", GitStatusKind.DELETED),
                ticked("Test Cases/a.testcase", GitStatusKind.ADDED)));

        final @NotNull List<String> committed = committedPaths();
        assertTrue(committed.toString(), committed.contains("Test Cases/a.testcase"));
        assertFalse("the path that is gone was committed as a removal: " + committed, committed.contains("Test Cases/login/a.testcase"));
        assertEquals("", mustGit(work, "status", "--porcelain", "-uall").strip());
    }

    // UC-SHARE-012, Rule-SHARE-058
    public void testACommitTooLongForTheCommandLineStillLands() {
        final @NotNull List<PendingChange> selected = new ArrayList<>();
        int asArguments = 0;
        for (int i = 0; i < 1200; i++) {
            final @NotNull String relativePath = "Test Cases/a package with a long name/a test set with a long name/" + UUID.randomUUID() + ".testcase";
            write(work, relativePath, "{}");
            selected.add(ticked(relativePath, GitStatusKind.ADDED));
            asArguments += relativePath.length() + 3;
        }
        assertTrue("the paths would fit on the command line, so this proves nothing", asArguments > WINDOWS_COMMAND_LINE_LIMIT);

        commits().stageAndCommit(work, "imported 1200 test cases", selected);

        assertEquals(1201, committedPaths().size());
        assertEquals("", mustGit(work, "status", "--porcelain", "-uall").strip());
    }

    // UC-SHARE-016, Rule-SHARE-071
    public void testThePullRebasesAndKeepsWhatWasNotCommitted() {
        final @NotNull Path colleague = colleague();
        write(colleague, "theirs.testcase", "{}");
        commitAll(colleague, "their test case");
        mustGit(colleague, "push", "origin", MAIN);
        final @NotNull String theirCommit = head(colleague, "HEAD");

        write(work, "mine.testcase", "{}");
        commitAll(work, "my test case");
        write(work, "first.testcase", "{\"typed\":\"not committed yet\"}");

        commits().pullWhereTheRemoteHasBranch(work, "origin", remoteUrl(), MAIN);

        assertEquals("my commit was replayed on top of theirs", theirCommit, head(work, "HEAD~1"));
        assertEquals("a rebase makes no merge commit", "", mustGit(work, "log", "--merges", "--oneline").strip());
        assertEquals("the uncommitted change was stashed and put back", "{\"typed\":\"not committed yet\"}", read(work, "first.testcase"));
    }

    // UC-SHARE-013, Rule-SHARE-123
    public void testCommitAndPushLandsOnTopOfWhatTheRemoteHolds() {
        final @NotNull Path colleague = colleague();
        write(colleague, "theirs.testcase", "{}");
        commitAll(colleague, "their test case");
        mustGit(colleague, "push", "origin", MAIN);
        final @NotNull String theirCommit = head(colleague, "HEAD");

        write(work, "mine.testcase", "{}");
        commitAll(work, "my test case");

        commits().pullAndPush(work, "origin", remoteUrl(), MAIN);

        assertEquals("the remote holds what was pushed", head(work, "HEAD"), head(remote, MAIN));
        assertEquals("my commit sits on top of theirs", theirCommit, head(work, "HEAD~1"));
    }

    // UC-SHARE-016, Rule-SHARE-125
    public void testARemoteThatCannotBeAskedStopsBeforeAnythingIsPulledOrPushed() {
        final @NotNull String before = head(remote, MAIN);
        final @NotNull String nowhere = root.resolve("nowhere.git").toUri().toString();
        mustGit(work, "remote", "set-url", "origin", nowhere);
        write(work, "mine.testcase", "{}");
        commitAll(work, "my test case");

        try {
            commits().pullAndPush(work, "origin", nowhere, MAIN);
            fail("a remote that cannot be asked was taken for one with no branch");
        } catch (final GitFailed refused) {
            final @NotNull String reason = String.valueOf(refused.getMessage());
            assertTrue(reason, reason.startsWith(Bundle.message("git.error.remote.unreachable", "origin", MAIN, "").strip()));
            assertTrue("the message gives the reason Git gave: " + reason, reason.length() > Bundle.message("git.error.remote.unreachable", "origin", MAIN, "").length());
        }

        assertEquals("nothing was pushed", before, head(remote, MAIN));
    }

    // UC-SHARE-017, Rule-SHARE-077
    public void testRollingBackKeepsEverythingThatWasHereBeforeThePull() {
        bothChangeTheNotes();
        write(work, "first.testcase", "{\"typed\":\"not committed yet\"}");
        final @NotNull String before = head(work, "HEAD");

        pullStopsOnAConflict();
        RebaseEnd.ABORT.runIn(new GitRepositoryService(getProject()), work);

        assertEquals("the commit that was here is here again", before, head(work, "HEAD"));
        assertEquals("mine", read(work, "notes.txt").strip());
        assertEquals("the uncommitted change came back", "{\"typed\":\"not committed yet\"}", read(work, "first.testcase"));
        assertTrue(new GitRepositoryService(getProject()).unfinished(work).isEmpty());
    }

    // UC-SHARE-012, Rule-SHARE-056
    public void testARebaseGoesOnWithoutOpeningAnEditor() {
        bothChangeTheNotes();
        pullStopsOnAConflict();

        write(work, "notes.txt", "mine and theirs\n");
        mustGit(work, "add", "notes.txt");
        final @NotNull GitRepositoryService repositories = new GitRepositoryService(getProject());

        assertFalse("the rebase stopped for an editor", repositories.couldNotContinueRebase(work));
        assertTrue(repositories.unfinished(work).isEmpty());
        assertEquals("mine and theirs", read(work, "notes.txt").strip());
    }

    // UC-SHARE-017, Rule-SHARE-079
    public void testAFileTestinCannotMergeIsNamedBackAsStillInTheWay() {
        bothChangeTheNotes();
        pullStopsOnAConflict();

        final @NotNull AtomicReference<List<String>> leftOver = new AtomicReference<>(List.of());
        final @NotNull AtomicBoolean resolved = new AtomicBoolean();
        final @NotNull List<String> conflicting = new GitRepositoryService(getProject()).conflictingPaths(work);
        ApplicationManager.getApplication().executeOnPooledThread(() -> ConflictResolution.resolve(getProject(), work, conflicting, () -> resolved.set(true), leftOver::set));

        Await.until("the file Testin cannot merge was not named back", () -> !Objects.requireNonNull(leftOver.get(), "nothing was named back").isEmpty() || resolved.get());
        assertEquals(List.of("notes.txt"), leftOver.get());
        assertTrue(git(work, "rebase", "--abort").isPresent());
    }
}
