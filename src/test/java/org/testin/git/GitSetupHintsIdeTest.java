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

import com.intellij.ui.components.ActionLink;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.git.review.ViewPendingCommitsWork;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testin.git.LocalGit.mustGit;

public class GitSetupHintsIdeTest extends AbstractGitRemoteIdeTest {

    private @NotNull List<SetupHint> hintsFor(final @NotNull SetupStep step) {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == step).toList();
    }

    // UC-SHARE-009, Rule-SHARE-042, Rule-INTERNAL-127
    public void testAFolderNotUnderGitAnswersBrieflyAndItsHintMakesTheRepository() {
        final @NotNull Path notARepository = directory("not-a-repository");
        write(notARepository, ".tp", "{}");
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        new ViewPendingCommitsWork(getProject()).openFor(notARepository);

        assertEquals("the gesture raised a lasting notification", List.of(), said.notifications());
        assertTrue("the gesture was not answered", said.shown().stream().anyMatch(shown -> shown.contains(Bundle.message("git.no.repository.title"))));
        assertEquals("the folder did not wait as one hint", 1, hintsFor(SetupStep.GIT_REPOSITORY).size());

        final @NotNull ActionLink initialize = (ActionLink) hintsFor(SetupStep.GIT_REPOSITORY).getFirst().form().orElseThrow().get();
        assertEquals(Bundle.message("git.no.repository.action"), initialize.getText());
        initialize.doClick();

        Await.until("Initialize Git did not clear its hint", () -> hintsFor(SetupStep.GIT_REPOSITORY).isEmpty());
        assertTrue("Initialize Git made no repository", Files.isDirectory(notARepository.resolve(".git")));
    }

    // UC-SHARE-016, Rule-INTERNAL-127
    public void testSyncWithNoRemoteAnswersBrieflyAndWaitsAsAHint() {
        final @NotNull Path alone = directory("alone");
        mustGit(alone, "init", "-q");
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        new SyncWork(getProject()).syncRepository(alone);

        Await.until("no remote did not wait as a hint", () -> hintsFor(SetupStep.GIT_REMOTE).size() == 1);
        assertEquals(Bundle.message("git.sync.aborted.message"), hintsFor(SetupStep.GIT_REMOTE).getFirst().text());
        assertEquals("the gesture raised a lasting notification", List.of(), said.notifications());
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.GIT_REMOTE);
    }

    // UC-SHARE-009, Rule-SHARE-042, Rule-INTERNAL-127
    public void testAnotherTestProjectUnderGitLeavesTheHintAndSyncAfterGitInitClearsIt() {
        final @NotNull Path notARepository = directory("not-a-repository");
        write(notARepository, ".tp", "{}");
        final @NotNull Path repository = directory("a-repository");
        mustGit(repository, "init", "-q");
        new ViewPendingCommitsWork(getProject()).openFor(notARepository);

        new ViewPendingCommitsWork(getProject()).openFor(repository);
        assertEquals("another test project under Git cleared the hint", 1, hintsFor(SetupStep.GIT_REPOSITORY).size());

        mustGit(notARepository, "init", "-q");
        new SyncWork(getProject()).syncRepository(notARepository);
        Await.until("Sync on the repository the hint named did not clear it", () -> hintsFor(SetupStep.GIT_REPOSITORY).isEmpty());
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.GIT_REMOTE);
    }
}
