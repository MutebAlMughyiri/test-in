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
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class ViewPendingCommitsWorkIdeTest extends AbstractGitRemoteIdeTest {
    private static final @NotNull String TEST_CASE = "Test Cases/Login/login.testcase";

    private static @NotNull String aTestCase(final @NotNull String description, final @NotNull String expected) {
        return "{\"description\":\"" + description + "\",\"expectedResult\":\"" + expected + "\"}";
    }

    private static @NotNull AnAction answer(final @NotNull Notification notification, final @NotNull String text) {
        return notification.getActions().stream().filter(action -> text.equals(action.getTemplateText())).findFirst()
                .orElseThrow(() -> new AssertionError("the message does not offer " + text + ": " + answers(notification)));
    }

    private void aPullStoppedOnATestCaseEachSideChangedInAnotherField() {
        write(work, TEST_CASE, aTestCase("Log in", "The dashboard opens"));
        commitAll(work, "the test case");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull Path colleague = colleague();
        write(colleague, TEST_CASE, aTestCase("Sign in", "The dashboard opens"));
        commitAll(colleague, "their wording");
        mustGit(colleague, "push", "origin", MAIN);

        write(work, TEST_CASE, aTestCase("Log in", "The account page opens"));
        commitAll(work, "my expected result");
        assertTrue("the pull did not stop on the test case", git(work, "pull", "--rebase", "origin", MAIN).isEmpty());
    }

    // UC-SHARE-015, Rule-SHARE-067
    public void testNothingToCommitAndNothingToPushSaysThereAreNoChanges() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        new ViewPendingCommitsWork(getProject()).openFor(work);

        Await.until("nothing to commit or push was not said", () -> said.shown().contains(Bundle.message("git.no.changes")));
    }

    // UC-SHARE-017, Rule-SHARE-076, Rule-SHARE-078
    public void testAPullStoppedPartWayIsOfferedBackAndResolvingItMergesContinuesAndPushes() {
        aPullStoppedOnATestCaseEachSideChangedInAnotherField();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new ViewPendingCommitsWork(getProject()).openFor(work);

        final @NotNull Notification stopped = titled(said, Bundle.message("git.conflicts.title"));
        assertEquals("a stopped pull does not offer its three answers", List.of(Bundle.message("git.conflicts.resolve"), Bundle.message("git.conflicts.continue.rebase"), Bundle.message("git.conflicts.abort.rebase")), answers(stopped));

        Notified.press(getProject(), stopped, answer(stopped, Bundle.message("git.conflicts.resolve")));

        titled(said, Bundle.message("git.rebase.continued.title"));
        assertEquals("the rebased commit did not reach the remote", head(work, "HEAD"), head(remote, MAIN));
        final @NotNull String merged = read(work, TEST_CASE);
        assertTrue("the pushed test case lost a side's change: " + merged, merged.contains("Sign in") && merged.contains("The account page opens"));
    }

    // UC-SHARE-015, Rule-SHARE-066, Rule-SHARE-067
    public void testCommitsNotOnTheRemoteAreCountedRatherThanReportedAsNoChanges() {
        write(work, "a.testcase", "{}");
        write(work, "b.testcase", "{}");
        write(work, "c.testcase", "{}");
        commitAll(work, "three test cases");
        write(work, "d.testcase", "{}");
        commitAll(work, "one more");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        new ViewPendingCommitsWork(getProject()).openFor(work);

        final @NotNull Notification waiting = titled(said, Bundle.message("git.not.pushed.title"));
        assertEquals("two commits holding four files", Bundle.message("git.not.pushed.many", "2"), waiting.getContent());
        assertEquals(List.of(Bundle.message("git.push.action")), answers(waiting));
    }
}
