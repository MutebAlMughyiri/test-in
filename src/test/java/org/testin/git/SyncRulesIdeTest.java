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
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.git.review.PendingChangesDialog;
import org.testin.git.review.ViewPendingCommitsWork;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.testin.git.LocalGit.mustGit;

public class SyncRulesIdeTest extends AbstractGitRemoteIdeTest {

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), PendingChangesDialog.class);
        super.tearDown();
    }

    private void theColleagueAdds(final @NotNull String relativePath, final @NotNull String content) {
        final @NotNull Path colleague = colleague();
        write(colleague, relativePath, content);
        commitAll(colleague, "their " + relativePath);
        mustGit(colleague, "push", "origin", MAIN);
    }

    private @NotNull Notification synced(final @NotNull List<Notification> said) {
        new SyncWork(getProject()).syncRepository(work);
        return titled(said, Bundle.message("git.synced.title"));
    }

    // UC-SHARE-016, Rule-SHARE-003
    public void testOneSyncSendsMyWorkAndTakesTheirs() {
        theColleagueAdds("theirs.testcase", "{}");
        write(work, "mine.testcase", "{}");
        commitAll(work, "my test case");

        synced(Said.listening(getProject(), getTestRootDisposable()).notifications());

        assertTrue("the colleague's work was not taken", Files.exists(work.resolve("theirs.testcase")));
        assertEquals("my work is still only on this machine", head(work, "HEAD"), head(remote, MAIN));
        assertEquals("{}", mustGit(remote, "show", MAIN + ":mine.testcase").trim());
    }

    // UC-SHARE-016, Rule-SHARE-070
    public void testThePushIsSkippedWhenNothingIsWaiting() {
        theColleagueAdds("theirs.testcase", "{}");
        mustGit(work, "remote", "set-url", "--push", "origin", root.resolve("nowhere.git").toUri().toString());

        final @NotNull Notification done = synced(Said.listening(getProject(), getTestRootDisposable()).notifications());

        assertEquals("a push was tried with nothing to send", Bundle.message("git.synced.up.to.date"), done.getContent());
        assertTrue("the sync did not take the colleague's work", Files.exists(work.resolve("theirs.testcase")));
    }

    // UC-SHARE-016, Rule-SHARE-005, Rule-SHARE-072
    public void testAGitStepThatOnlyReadsCanBeCanceledAndOneThatWritesCannot() {
        final @NotNull List<Task> started = ShareGestures.tasksStarted(getTestRootDisposable());

        synced(Said.listening(getProject(), getTestRootDisposable()).notifications());
        write(work, "mine.testcase", "{\"description\":\"typed\"}");
        new ViewPendingCommitsWork(getProject()).openFor(work);

        assertFalse("the sync, which pulls with a rebase and pushes, could be canceled", ShareGestures.titled(started, Bundle.message("git.task.syncing")).isCancellable());
        assertTrue("reading the changes could not be canceled", ShareGestures.titled(started, Bundle.message("git.task.scanning")).isCancellable());
    }

    // UC-SHARE-010, Rule-SHARE-050
    public void testReadingGitHappensOffTheMainThread() {
        final @NotNull List<Boolean> onTheMainThread = ShareGestures.gitCallsOnTheMainThread(getTestRootDisposable());
        write(work, "mine.testcase", "{\"description\":\"typed\"}");

        new ViewPendingCommitsWork(getProject()).openFor(work);
        ShownDialog.waitedFor(getProject(), PendingChangesDialog.class);

        assertFalse("Git was never asked", onTheMainThread.isEmpty());
        assertFalse("Git was read on the main thread", onTheMainThread.contains(true));
    }

    // UC-SHARE-016, Rule-SHARE-073
    public void testAfterTheSyncTestinReadsTheWorkingFolderAgain() {
        final @NotNull TestProjectNode demo = new NodesOnDisk(getProject()).testProject(work);
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(demo.getTestCasesFolder(), "Login");
        new NodesOnDisk(getProject()).testCase(login);
        commitAll(work, "the Login test set");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull TestCase theirs = TestCase.builder().id(UUID.randomUUID()).description("sign in with a passkey").order("z").build();
        final @NotNull String relative = work.relativize(login.getPath()).resolve(theirs.getId() + ".testcase").toString().replace('\\', '/');
        theColleagueAdds(relative, Services.getInstance(getProject(), JsonCodec.class).writeValueAsString(theirs));

        synced(Said.listening(getProject(), getTestRootDisposable()).notifications());

        Await.until("Testin's reading of the folder does not hold the colleague's test case", () -> Services.getInstance(getProject(), TestCases.class).findTestCase(theirs.getId()).isPresent());
        assertNotNull("the IDE's view of the folder was not read again", LocalFileSystem.getInstance().findFileByNioFile(work.resolve(relative)));
    }
}
