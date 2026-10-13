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
