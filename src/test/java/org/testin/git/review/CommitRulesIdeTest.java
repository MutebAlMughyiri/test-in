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

package org.testin.git.review;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.config.TestinYml;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.git.ShareGestures;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.JRadioButton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class CommitRulesIdeTest extends AbstractGitRemoteIdeTest {

    private static @NotNull String addressOf(final @NotNull Path bare) {
        final @NotNull String address = bare.toUri().toString();
        return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
    }

    private static long count(final @NotNull List<Notification> said, final @NotNull String title) {
        return said.stream().filter(notification -> notification.getTitle().equals(title)).count();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), PendingChangesDialog.class);
        ShownDialog.close(getProject(), GitIdentityDialog.class);
        ShownDialog.close(getProject(), RemoteUrlDialog.class);
        noTestinYml();
        super.tearDown();
    }

    private void noTestinYml() {
        final @NotNull Optional<Path> yml = TestinYml.savePath(getProject());
        try {
            if (yml.isPresent()) Files.deleteIfExists(yml.orElseThrow());
        } catch (final IOException ex) {
            throw new AssertionError("Could not remove testin.yml: " + ex.getMessage(), ex);
        }
        yml.map(Path::getParent).ifPresent(LocalFileSystem.getInstance()::refreshAndFindFileByNioFile);
        TestinYml.reload(getProject());
    }

    private @NotNull JComponent theReview(final @NotNull Path repository) {
        return ShareGestures.theReviewOf(getProject(), repository);
    }

    private @NotNull Path aRepositoryWithNoRemote() {
        final @NotNull Path solo = directory("solo");
        mustGit(solo, "init", "--initial-branch=" + MAIN);
        identify(solo, "Testin Test", "testin@example.invalid");
        write(solo, "first.testcase", "{}");
        commitAll(solo, "first");
        return solo;
    }

    private @NotNull Path anEmptyRemote() {
        final @NotNull Path empty = directory("empty.git");
        mustGit(empty, "init", "--bare", "--initial-branch=" + MAIN);
        return empty;
    }

    private void theTestProjectFolderExists() {
        final @NotNull Path folder = TestinYml.savePath(getProject()).map(Path::getParent).orElseThrow(() -> new AssertionError("the project has no folder for testin.yml"));
        try {
            Files.createDirectories(folder);
        } catch (final IOException ex) {
            throw new AssertionError("Could not make " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private void pressPushOnTheWaitingCommits(final @NotNull Path repository, final @NotNull List<Notification> said) {
        final int before = (int) said.stream().filter(notification -> notification.getTitle().equals(Bundle.message("git.not.pushed.title"))).count();
        new ViewPendingCommitsWork(getProject()).openFor(repository);
        Await.until("the waiting commits were not offered", () -> said.stream().filter(notification -> notification.getTitle().equals(Bundle.message("git.not.pushed.title"))).count() > before);

        final @NotNull Notification waiting = said.stream().filter(notification -> notification.getTitle().equals(Bundle.message("git.not.pushed.title"))).toList().getLast();
        Notified.press(getProject(), waiting, waiting.getActions().getFirst());
    }

    // UC-SHARE-008, Rule-SHARE-040
    public void testTheCommitIsMadeStraightAfterTheIdentityIsSet() {
        mustGit(work, "config", "user.name", "");
        mustGit(work, "config", "user.email", "");
        write(work, "second.testcase", "{\"description\":\"typed\"}");
        final @NotNull String before = head(work, "HEAD");

        final @NotNull JComponent review = theReview(work);
        ShareGestures.type(review, "the second test case");
        ShareGestures.commitOnly(review);

        final @NotNull JComponent identity = ShownDialog.waitedFor(getProject(), GitIdentityDialog.class);
        assertTrue("the identity would be written for every repository on this machine", Drawn.first(identity, JRadioButton.class, radio -> radio.getText().equals(Bundle.message("dialog.git.identity.option.repository"))).isSelected());
        ShareGestures.typeInto(identity, 0, "Sara Tester");
        ShareGestures.typeInto(identity, 1, "sara@example.invalid");
        ShareGestures.pressEnter(identity);

        Await.until("no commit followed the identity", () -> !head(work, "HEAD").equals(before));
        assertEquals("Sara Tester", mustGit(work, "log", "-1", "--format=%an").trim());
        assertEquals("the second test case", mustGit(work, "log", "-1", "--format=%s").trim());
    }

    // UC-SHARE-013, Rule-SHARE-059, Rule-SHARE-127
    public void testThePushHappensOnlyAfterTheCommitSucceeded() {
        write(work, ".git/hooks/pre-commit", """
                #!/bin/sh
                exit 1
                """);
        write(work, "second.testcase", "{\"description\":\"typed\"}");
        final @NotNull String remoteBefore = head(remote, MAIN);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        final @NotNull List<Task> started = ShareGestures.tasksStarted(getTestRootDisposable());

        final @NotNull JComponent review = theReview(work);
        ShareGestures.type(review, "the second test case");
        ShareGestures.commitAndPush(review);

        final @NotNull Notification failed = titled(said, Bundle.message("git.commit.failed.title"));
        assertTrue("the failed commit offers nothing to press", failed.getActions().stream().anyMatch(action -> Bundle.message("git.show.log").equals(action.getTemplatePresentation().getText())));
        assertEquals("something reached the remote", remoteBefore, head(remote, MAIN));
        assertTrue("a push was started after a failed commit", started.stream().noneMatch(task -> task.getTitle().equals(Bundle.message("git.task.checking.remote")) || task.getTitle().equals(Bundle.message("git.task.pushing.remote"))));
    }

    // UC-SHARE-013, Rule-SHARE-060
    public void testARepositoryWithNoRemoteTakesTheAddressTestinYmlGives() {
        final @NotNull Path solo = aRepositoryWithNoRemote();
        final @NotNull Path empty = anEmptyRemote();
        theTestProjectFolderExists();
        assertTrue("could not write testin.yml", TestinYml.save(getProject(), TestinYml.lines("solo", addressOf(empty))));
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        pressPushOnTheWaitingCommits(solo, said);

        titled(said, Bundle.message("git.pushed.title"));
        assertEquals(head(solo, "HEAD"), head(empty, MAIN));
        assertFalse("the address was asked for although testin.yml gives it", ShownDialog.isOpen(getProject(), RemoteUrlDialog.class));
    }

    // UC-SHARE-013, Rule-SHARE-060
    public void testARepositoryWithNoRemoteAndNoAddressAsksForOneOnce() {
        noTestinYml();
        final @NotNull Path solo = aRepositoryWithNoRemote();
        final @NotNull Path empty = anEmptyRemote();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        pressPushOnTheWaitingCommits(solo, said);
        final @NotNull JComponent asked = ShownDialog.waitedFor(getProject(), RemoteUrlDialog.class);
        ShareGestures.typeInto(asked, 0, addressOf(empty));
        ShareGestures.pressEnter(asked);
        Await.until("the first push did not land", () -> count(said, Bundle.message("git.pushed.title")) == 1);

        write(solo, "second.testcase", "{}");
        commitAll(solo, "second");
        pressPushOnTheWaitingCommits(solo, said);
        Await.until("the second push did not land", () -> count(said, Bundle.message("git.pushed.title")) == 2);

        assertFalse("the address was asked for a second time", ShownDialog.isOpen(getProject(), RemoteUrlDialog.class));
        assertEquals(head(solo, "HEAD"), head(empty, MAIN));
    }

    // UC-SHARE-013, Rule-SHARE-061
    public void testTheMessageAboutAPushStaysInTheNotificationList() {
        write(work, "second.testcase", "{}");
        commitAll(work, "second");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        pressPushOnTheWaitingCommits(work, said);

        final @NotNull Notification pushed = titled(said, Bundle.message("git.pushed.title"));
        assertTrue("the push message is not kept in the notification list", NotificationGroupManager.getInstance().getNotificationGroup(pushed.getGroupId()).isLogByDefault());
        assertEquals(head(work, "HEAD"), head(remote, MAIN));
    }

    // UC-SHARE-013, Rule-SHARE-127
    public void testAPushTheRemoteRefusesSaysWhyAndTriesAgainWhenAsked() {
        write(remote, "hooks/pre-receive", """
                #!/bin/sh
                echo "pushes are closed today" >&2
                exit 1
                """);
        write(work, "second.testcase", "{}");
        commitAll(work, "second");
        final @NotNull String before = head(remote, MAIN);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        pressPushOnTheWaitingCommits(work, said);

        final @NotNull Notification failed = titled(said, Bundle.message("git.push.failed.title"));
        assertTrue("the refusal did not say what the remote said: " + failed.getContent(), failed.getContent().contains("pushes are closed today"));
        final @NotNull AnAction tryAgain = failed.getActions().stream().filter(action -> Bundle.message("git.try.again").equals(action.getTemplateText())).findFirst().orElseThrow(() -> new AssertionError("the refusal offers no Try Again: " + answers(failed)));
        Notified.press(getProject(), failed, tryAgain);
        Await.until("Try Again did not push again", () -> count(said, Bundle.message("git.push.failed.title")) == 2);
        assertEquals("a refused push moved the remote", before, head(remote, MAIN));
    }

    // UC-SHARE-013, Rule-SHARE-068
    public void testAPushWithNoBranchCheckedOutPushesNothingAndSaysSo() {
        final @NotNull Path colleague = colleague();
        mustGit(colleague, "checkout", "--detach");
        write(colleague, "theirs.testcase", "{}");
        commitAll(colleague, "a test case on no branch");
        final @NotNull String before = head(remote, MAIN);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        pressPushOnTheWaitingCommits(colleague, said);

        final @NotNull Notification refused = titled(said, Bundle.message("git.error.title"));
        assertTrue(refused.getContent(), refused.getContent().contains(Bundle.message("git.error.no.push.branch")));
        assertEquals("a push with no branch checked out moved the remote", before, head(remote, MAIN));
    }

    // UC-SHARE-014, Rule-SHARE-064
    public void testANameThatIsNotABranchYetStartsOneAndTheCommitLandsOnIt() {
        write(work, "first.testcase", "{\"on\":\"the release\"}");
        final @NotNull String mainBefore = head(work, MAIN);

        final @NotNull JComponent review = theReview(work);
        ShareGestures.typeTheBranch(review, "release");
        ShareGestures.type(review, "the release's first change");
        ShareGestures.commitOnly(review);

        Await.until("the commit did not land on a new branch named release", () -> git(work, "rev-parse", "--verify", "--quiet", "release").filter(hash -> !hash.trim().equals(mainBefore.trim())).isPresent());
        assertEquals("the new branch is not the one checked out", "release", mustGit(work, "branch", "--show-current").trim());
        assertEquals("the commit on the new branch moved main", mainBefore, head(work, MAIN));
    }

    // UC-SHARE-014, Rule-SHARE-065, Rule-SHARE-127
    public void testABranchThatCannotBeCheckedOutCommitsNothing() {
        mustGit(work, "checkout", "-b", "other");
        write(work, "first.testcase", "{\"on\":\"other\"}");
        commitAll(work, "the other branch's first test case");
        mustGit(work, "checkout", MAIN);
        write(work, "first.testcase", "{\"on\":\"typed on main\"}");
        final @NotNull String mainBefore = head(work, MAIN);
        final @NotNull String otherBefore = head(work, "other");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        final @NotNull JComponent review = theReview(work);
        ShareGestures.chooseTheOtherBranch(review);
        ShareGestures.type(review, "the typed test case");
        ShareGestures.commitOnly(review);

        final @NotNull Notification refused = titled(said, Bundle.message("git.branch.not.switched.title"));
        assertTrue("the refusal did not say what Git said: " + refused.getContent(), refused.getContent().contains("first.testcase"));
        assertTrue("the refusal offers no Show Git log", refused.getActions().stream().anyMatch(action -> Bundle.message("git.show.log").equals(action.getTemplateText())));
        assertEquals(mainBefore, head(work, MAIN));
        assertEquals(otherBefore, head(work, "other"));
        assertEquals(MAIN, mustGit(work, "branch", "--show-current").trim());
        assertEquals("{\"on\":\"typed on main\"}", read(work, "first.testcase"));
        assertTrue(git(work, "diff", "--quiet").isEmpty());
    }
}
