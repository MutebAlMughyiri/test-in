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

import org.jetbrains.annotations.NotNull;
import org.testin.git.change.ChangeSubject;
import org.testin.git.change.StatusKind;
import org.testin.git.change.PendingChangeReader;
import org.testin.git.change.PendingChange;
import org.testin.model.TestCase;
import org.testin.util.RealJsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public class GitRepositoryServiceIdeTest extends AbstractGitRemoteIdeTest {

    private @NotNull GitRepositoryService repositories() {
        return new GitRepositoryService(getProject());
    }

    // UC-SHARE-008, Rule-SHARE-039
    public void testAnIdentityIsAskedForOnlyWhenGitHasNone() {
        assertFalse("a name and an email address are set for this repository", repositories().hasNoIdentity(work));

        mustGit(work, "config", "user.email", "");

        assertTrue("an email address left empty is no email address", repositories().hasNoIdentity(work));
    }

    // UC-SHARE-009, Rule-SHARE-043
    public void testMakingTheRepositoryCommitsNothing() {
        final @NotNull Path fresh = directory("fresh");
        write(fresh, ".tp", "{}");
        write(fresh, "Test Cases/login/a.tc", "{}");

        repositories().initialize(fresh);

        assertTrue("the repository was made", Files.isDirectory(fresh.resolve(".git")));
        assertTrue("nothing was committed by making it", git(fresh, "rev-parse", "--verify", "HEAD").isEmpty());
        final @NotNull String waiting = mustGit(fresh, "status", "--porcelain", "-uall");
        assertTrue("the test project is still waiting to be committed: " + waiting, waiting.contains("Test Cases/login/a.tc"));
    }

    // UC-SHARE-010, Rule-SHARE-044
    public void testTheReviewReadsGitForARepositoryTheIdeDoesNotTrack() {
        final @NotNull TestCase neverSeen = TestCase.builder().description("a test case Git has never seen").build();
        write(work, "Test Cases/login/" + neverSeen.getId() + ".tc", RealJsonCodec.build().writeValueAsString(neverSeen));

        final @NotNull List<PendingChange> review = PendingChangeReader.getPendingChanges(getProject(), work);

        assertEquals(1, review.size());
        assertEquals(ChangeSubject.TEST_CASE, review.getFirst().subject());
        assertEquals(StatusKind.ADDED, review.getFirst().type());
        assertEquals("a test case Git has never seen", review.getFirst().name());
    }

    // UC-SHARE-014, Rule-SHARE-063
    public void testTheBoxListsTheBranchesOnThisMachine() {
        mustGit(work, "branch", "feature/login");
        mustGit(work, "branch", "release");
        mustGit(work, "fetch", "origin");

        assertEquals("the remote's branches are not on this machine", List.of("feature/login", MAIN, "release"), repositories().getLocalBranches(work));
        assertEquals(MAIN, repositories().getCurrentBranch(work));
    }

    // UC-SHARE-014, Rule-SHARE-064
    public void testANameThatIsNotABranchYetStartsOne() {
        assertTrue(repositories().startBranch(work, "feature/new-test-cases"));

        assertEquals("feature/new-test-cases", repositories().getCurrentBranch(work));
        assertTrue(repositories().getLocalBranches(work).contains("feature/new-test-cases"));
    }

    // UC-SHARE-015, Rule-SHARE-066
    public void testCommitsWaitingForTheRemoteAreCountedNotTheirFiles() {
        assertEquals(OptionalInt.of(0), repositories().unpushedCount(work));

        write(work, "a.tc", "{}");
        write(work, "b.tc", "{}");
        write(work, "c.tc", "{}");
        commitAll(work, "three test cases");
        write(work, "d.tc", "{}");
        commitAll(work, "one more");

        assertEquals("two commits holding four files", OptionalInt.of(2), repositories().unpushedCount(work));
    }

    // UC-SHARE-016, Rule-SHARE-068
    public void testTheBranchSyncedIsTheOneCheckedOut() {
        mustGit(work, "fetch", "origin");
        mustGit(work, "remote", "set-head", "origin", MAIN);
        mustGit(work, "checkout", "-b", "feature/login");

        assertEquals("feature/login", repositories().syncBranch(work));
    }

    // UC-SHARE-016, Rule-SHARE-122
    public void testOriginIsTheRemoteUsedAmongSeveral() {
        mustGit(work, "remote", "add", "aaa-upstream", root.resolve("upstream.git").toUri().toString());

        assertEquals("origin", repositories().getRemoteName(work));

        mustGit(work, "remote", "remove", "origin");
        mustGit(work, "remote", "add", "zzz-fork", root.resolve("fork.git").toUri().toString());

        assertEquals("with no origin, the first remote Git lists", "aaa-upstream", repositories().getRemoteName(work));
    }
}
