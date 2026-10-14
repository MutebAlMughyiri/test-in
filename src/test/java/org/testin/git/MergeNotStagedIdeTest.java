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
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.git.conflict.ConflictResolution;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class MergeNotStagedIdeTest extends AbstractGitRemoteIdeTest {
    private static final @NotNull String TEST_CASE = "Test Cases/Login/login.testcase";

    private static @NotNull String oneLine(final @NotNull String description, final @NotNull String expected) {
        return "{\"description\":\"" + description + "\",\"expectedResult\":\"" + expected + "\"}";
    }

    private static void deleteTheLock(final @NotNull Path lock) {
        try {
            Files.deleteIfExists(lock);
        } catch (final IOException ex) {
            throw new AssertionError("Could not remove the index lock: " + ex.getMessage(), ex);
        }
    }

    private void bothChangeTheTestCase() {
        write(work, TEST_CASE, oneLine("Log in", "The dashboard opens"));
        commitAll(work, "the test case");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull Path colleague = colleague();
        write(colleague, TEST_CASE, oneLine("Sign in", "The dashboard opens"));
        commitAll(colleague, "their wording");
        mustGit(colleague, "push", "origin", MAIN);

        write(work, TEST_CASE, oneLine("Log in", "The account page opens"));
        commitAll(work, "my expected result");
        assertTrue("the pull did not stop on the test case", git(work, "pull", "--rebase", "origin", MAIN).isEmpty());
        assertEquals(List.of(TEST_CASE), new GitRepositoryService(getProject()).conflictingPaths(work));
    }

    // UC-SHARE-017, Rule-SHARE-074
    public void testAMergedTestCaseGitWillNotStageIsReportedAndStopsTheSync() {
        bothChangeTheTestCase();
        final @NotNull Path lock = work.resolve(".git").resolve("index.lock");
        write(work, ".git/index.lock", "");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        final @NotNull List<List<String>> leftOver = new CopyOnWriteArrayList<>();
        final @NotNull List<String> finished = new CopyOnWriteArrayList<>();

        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> ConflictResolution.resolve(getProject(), work, List.of(TEST_CASE), () -> finished.add("resolved"), leftOver::add));

            Await.until("the sync was told nothing was left over", () -> !leftOver.isEmpty() || !finished.isEmpty());
            final @NotNull Notification refused = titled(said, Bundle.message("git.merge.not.accepted.title"));
            assertEquals(Bundle.message("git.merge.not.accepted.message", TEST_CASE), refused.getContent());
            assertEquals(List.of(List.of(TEST_CASE)), leftOver);
            assertTrue("the sync went on without the test case", finished.isEmpty());
        } finally {
            deleteTheLock(lock);
            git(work, "rebase", "--abort");
        }
    }
}
