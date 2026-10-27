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
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.git.GitRepositoryService;
import org.testin.util.Bundle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testin.git.LocalGit.mustGit;

public class BranchSwitchToNewIdeTest extends AbstractGitRemoteIdeTest {

    private final @NotNull List<String> wentOn = new CopyOnWriteArrayList<>();

    private void startedOnANew(final @NotNull String branch) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> new BranchSwitch(getProject(), new GitRepositoryService(getProject())).toNew(work, branch, () -> wentOn.add(branch)));
    }

    private @NotNull String checkedOut() {
        return mustGit(work, "branch", "--show-current").trim();
    }

    // UC-SHARE-014, Rule-SHARE-064
    public void testANewBranchIsStartedAndCheckedOutBeforeTheCommitGoesOn() {
        startedOnANew("feature-login");

        Await.until("the commit did not go on once the branch was started", () -> !wentOn.isEmpty());
        assertEquals("the new branch is not the one checked out", "feature-login", checkedOut());
    }

    // UC-SHARE-014, Rule-SHARE-065, Rule-SHARE-127
    public void testABranchNameGitRefusesIsReportedAndTheCommitDoesNotGoOn() {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        startedOnANew("bad..name");

        titled(said, Bundle.message("git.branch.not.switched.title"));
        assertEquals("the commit went on though the branch was refused", List.of(), wentOn);
        assertEquals("a refused branch moved the repository off its branch", MAIN, checkedOut());
    }
}
