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

import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class SyncWorkIdeTest extends AbstractGitRemoteIdeTest {

    private void bothChangeTheNotes() {
        write(work, "notes.txt", "base\n");
        commitAll(work, "the common ancestor");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull Path colleague = colleague();
        write(colleague, "notes.txt", "theirs\n");
        commitAll(colleague, "their notes");
        mustGit(colleague, "push", "origin", MAIN);

        write(work, "notes.txt", "mine\n");
        commitAll(work, "my notes");
    }

    // UC-SHARE-016, UC-SHARE-017, Rule-SHARE-076, Rule-SHARE-078
    public void testResolvingThePullASyncStoppedOnFinishesTheSyncAndPushes() {
        bothChangeTheLoginTestCaseInAnotherField();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);
        final @NotNull Notification stopped = titled(said, Bundle.message("git.conflicts.title"));
        Notified.press(getProject(), stopped, stopped.getActions().getFirst());

        titled(said, Bundle.message("git.synced.title"));
        assertEquals("the resolved commit did not reach the remote", head(work, "HEAD"), head(remote, MAIN));
        assertTrue("the synced test case lost a side's change: " + read(work, LOGIN_TEST_CASE), read(work, LOGIN_TEST_CASE).contains("Sign in") && read(work, LOGIN_TEST_CASE).contains("The account page opens"));
    }

    // UC-SHARE-017, Rule-SHARE-127
    public void testAPushTheRemoteRefusesAfterTheSyncsPullIsResolvedSaysWhy() {
        bothChangeTheLoginTestCaseInAnotherField();
        write(remote, "hooks/pre-receive", """
                #!/bin/sh
                echo "pushes are closed today" >&2
                exit 1
                """);
        final @NotNull String before = head(remote, MAIN);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        new SyncWork(getProject()).syncRepository(work);
        final @NotNull Notification stopped = titled(said, Bundle.message("git.conflicts.title"));

        Notified.press(getProject(), stopped, stopped.getActions().getFirst());

        final @NotNull Notification failed = titled(said, Bundle.message("git.push.failed.title"));
        assertTrue("the refusal did not say what the remote said: " + failed.getContent(), failed.getContent().contains("pushes are closed today"));
        assertEquals("a refused push moved the remote", before, head(remote, MAIN));
    }

    // UC-SHARE-016, Rule-SHARE-042
    public void testAFolderNotUnderGitHasNothingToSyncAndSaysSo() {
        final @NotNull Path notARepository = directory("not-a-repository");
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        new SyncWork(getProject()).syncRepository(notARepository);

        assertEquals("a folder not under Git did not say there is nothing to sync", List.of(Bundle.message("git.sync.nothing.title") + "\n" + Bundle.message("git.sync.nothing.message", "not-a-repository")), said.shown());
    }

    // UC-SHARE-017, Rule-SHARE-075, Rule-SHARE-076
    public void testAPullThatStopsSaysSoAndOffersThreeAnswers() {
        bothChangeTheNotes();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);

        final @NotNull Notification conflicts = titled(said, Bundle.message("git.conflicts.title"));
        assertTrue(conflicts.getContent(), conflicts.getContent().contains("notes.txt"));
        assertEquals(List.of(Bundle.message("git.conflicts.resolve"), Bundle.message("git.conflicts.continue.rebase"), Bundle.message("git.conflicts.abort.rebase")), answers(conflicts));
        assertTrue("the repository is left part way through the pull", new GitRepositoryService(getProject()).unfinished(work).isPresent());

        assertTrue(git(work, "rebase", "--abort").isPresent());
    }

    // UC-SHARE-017, Rule-SHARE-076, Rule-SHARE-077
    public void testRollingBackAStoppedPullPutsTheRepositoryBackAndSaysSo() {
        bothChangeTheNotes();
        final @NotNull String mine = head(work, "HEAD");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);
        final @NotNull Notification stopped = titled(said, Bundle.message("git.conflicts.title"));
        Notified.press(getProject(), stopped, stopped.getActions().getLast());

        assertEquals("rolling back did not say what it did", Bundle.message("git.rebase.aborted.pull.message"), titled(said, Bundle.message("git.rebase.aborted.title")).getContent());
        assertEquals("rolling back did not keep what was here before the pull", mine, head(work, "HEAD"));
        assertFalse("rolling back left the pull part way", new GitRepositoryService(getProject()).unfinished(work).isPresent());
    }

    // UC-SHARE-017, Rule-SHARE-076, Rule-SHARE-079
    public void testCarryingOnWithAConflictStillInTheWayOffersThePullAgain() {
        bothChangeTheNotes();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);
        final @NotNull Notification first = titled(said, Bundle.message("git.conflicts.title"));
        Notified.press(getProject(), first, first.getActions().get(1));

        Await.until("carrying on with a conflict in the way did not offer the pull again", () -> said.stream().filter(notification -> notification.getTitle().equals(first.getTitle())).count() == 2);
        assertTrue(git(work, "rebase", "--abort").isPresent());
    }

    // UC-SHARE-017, Rule-SHARE-079
    public void testAPullThatWillNotMoveOnIsSaidAgain() {
        bothChangeTheNotes();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);
        final @NotNull Notification first = titled(said, Bundle.message("git.conflicts.title"));

        final @NotNull AnAction resolve = first.getActions().getFirst();
        Notified.press(getProject(), first, resolve);

        Await.until("the Git Conflicts message did not come back", () -> said.stream().filter(notification -> notification.getTitle().equals(first.getTitle())).count() == 2);
        final @NotNull Notification again = said.stream().filter(notification -> notification.getTitle().equals(first.getTitle())).toList().getLast();
        assertTrue("the message names the file still in the way: " + again.getContent(), again.getContent().contains("notes.txt"));

        assertTrue(git(work, "rebase", "--abort").isPresent());
    }

    // UC-SHARE-017, Rule-SHARE-075, Rule-SHARE-076
    public void testAPullStoppedBeforeTheSyncIsOfferedBackEvenWithNothingLeftConflicting() {
        bothChangeTheNotes();
        assertTrue("the pull did not stop on the notes", git(work, "pull", "--rebase", "origin", MAIN).isEmpty());
        write(work, "notes.txt", "both\n");
        mustGit(work, "add", "notes.txt");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);

        final @NotNull Notification stopped = titled(said, Bundle.message("git.conflicts.title"));
        assertEquals("a stopped pull with nothing left conflicting was not offered back", Bundle.message("git.conflict.unnamed"), stopped.getContent());
        assertTrue(git(work, "rebase", "--abort").isPresent());
    }

    // UC-SHARE-016, Rule-SHARE-068, Rule-SHARE-069
    public void testASyncWithNoBranchCheckedOutFailsAndSaysSo() {
        final @NotNull Path colleague = colleague();
        mustGit(colleague, "checkout", "--detach");
        write(colleague, "theirs.testcase", "{}");
        commitAll(colleague, "a test case on no branch");
        final @NotNull String before = head(remote, MAIN);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(colleague);

        final @NotNull Notification failed = titled(said, Bundle.message("git.sync.failed.title"));
        assertTrue(failed.getContent(), failed.getContent().contains(Bundle.message("git.error.no.sync.branch")));
        assertEquals("a sync with no branch checked out moved the remote", before, head(remote, MAIN));
    }

    // UC-SHARE-016, Rule-SHARE-125
    public void testARemoteThatCannotBeAskedStopsTheSyncAndSaysWhy() {
        final @NotNull String before = head(remote, MAIN);
        mustGit(work, "remote", "set-url", "origin", root.resolve("nowhere.git").toUri().toString());
        write(work, "mine.testcase", "{}");
        commitAll(work, "my test case");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new SyncWork(getProject()).syncRepository(work);

        final @NotNull Notification failed = titled(said, Bundle.message("git.sync.failed.title"));
        final @NotNull String unreachable = Bundle.message("git.error.remote.unreachable", "origin", MAIN, "").strip();
        assertTrue(failed.getContent(), failed.getContent().contains(unreachable.substring(0, unreachable.length() - 1)));
        assertEquals("nothing was pushed", before, head(remote, MAIN));
    }
}
