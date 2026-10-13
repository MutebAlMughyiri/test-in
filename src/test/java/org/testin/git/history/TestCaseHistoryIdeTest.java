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

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.git.AbstractGitRemoteIdeTest;
import org.testin.indexer.TestCaseFile;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.JsonCodec;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.testin.git.LocalGit.mustGit;

public class TestCaseHistoryIdeTest extends AbstractGitRemoteIdeTest {

    private static final @NotNull UUID ID = UUID.fromString("77777777-7777-4777-8777-777777777151");
    private static final @NotNull String FILE = ID + ".testcase";

    private @NotNull TestCase version(final @NotNull String expectedResult) {
        return TestCase.builder().id(ID).description("Log in with a valid user").expectedResult(expectedResult).createdBy("Muteb").build();
    }

    private void save(final @NotNull String relativePath, final @NotNull TestCase testCase) {
        write(work, relativePath, Services.getInstance(getProject(), JsonCodec.class).writeValueAsString(testCase));
    }

    private @NotNull History historyOf(final @NotNull Path testProjectPath, final @NotNull String relativePath, final @NotNull TestCase now) {
        try {
            return ApplicationManager.getApplication().executeOnPooledThread(() -> TestCaseHistory.read(getProject(), new TestCaseFile(testProjectPath, Path.of(relativePath)), Optional.of(now))).get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new AssertionError("the history was never read", ex);
        }
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-097, Rule-VIEW-PANEL-098
    public void testTheHistoryIsEveryCommitOnThisBranchNewestFirstFollowingAMoveWithTheUncommittedEditOnTop() {
        save("Test Cases/Login/" + FILE, version("The dashboard opens"));
        commitAll(work, "UC-10");
        save("Test Cases/Login/" + FILE, version("The dashboard opens within 2 seconds"));
        commitAll(work, "Cycle 4 review");
        mustGit(work, "mv", "Test Cases/Login", "Test Cases/Sign in");
        commitAll(work, "Renamed the test set");

        mustGit(work, "checkout", "-b", "side");
        save("Test Cases/Sign in/" + FILE, version("Changed on another branch"));
        commitAll(work, "Side branch");
        mustGit(work, "checkout", MAIN);

        final @NotNull TestCase now = version("The home page opens");
        save("Test Cases/Sign in/" + FILE, now);
        final @NotNull String onDisk = read(work, "Test Cases/Sign in/" + FILE);

        final @NotNull List<TestCaseHistoryCard> entries = historyOf(work, "Test Cases/Sign in/" + FILE, now).entries();

        assertEquals("expected Not committed yet, the move, the review and Created: " + entries, 4, entries.size());
        assertFalse(entries.getFirst().isCommitted());
        assertEquals("The home page opens", entries.getFirst().changes().getFirst().newValue());
        assertEquals("Renamed the test set", entries.get(1).message());
        assertTrue("a move changes no field", entries.get(1).changes().isEmpty());
        assertEquals("Cycle 4 review", entries.get(2).message());
        assertEquals("The dashboard opens within 2 seconds", entries.get(2).changes().getFirst().newValue());
        assertEquals(TestCaseHistoryKind.CREATED, entries.get(3).kind());
        assertEquals(head(work, "HEAD~1"), entries.get(2).hash());
        assertEquals(7, entries.get(2).shortHash().length());
        assertTrue("a commit on another branch reached the history", entries.stream().noneMatch(entry -> entry.message().equals("Side branch")));
        assertEquals("reading the history changed the file", onDisk, read(work, "Test Cases/Sign in/" + FILE));
    }

    // Rule-VIEW-PANEL-100
    public void testAFolderNotUnderGitHasNoHistory() {
        final @NotNull Path alone = directory("alone");

        assertEquals(History.NOT_UNDER_GIT, historyOf(alone, FILE, version("The dashboard opens")));
    }
}
