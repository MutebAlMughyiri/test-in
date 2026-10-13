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
import org.testin.AbstractTempRootIdeTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testin.git.LocalGit.mustGit;

public class SyncToANewBranchIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String BRANCH = "feature/new-test-cases";

    private Path remote;
    private Path work;

    @Override
    protected void setUp() {
        super.setUp();
        assertTrue("Git is not on the PATH, so a sync cannot be exercised", LocalGit.onThePath());

        try {
            remote = Files.createDirectories(root.resolve("remote.git"));
            work = Files.createDirectories(root.resolve("work"));
            Files.writeString(work.resolve("first.testcase"), "{}");
        } catch (final IOException ex) {
            throw new AssertionError("Could not lay out the repositories: " + ex.getMessage(), ex);
        }

        mustGit(remote, "init", "--bare", "--initial-branch=main");
        mustGit(work, "init", "--initial-branch=main");
        mustGit(work, "config", "user.name", "Testin Test");
        mustGit(work, "config", "user.email", "testin@example.invalid");
        mustGit(work, "remote", "add", "origin", remoteUrl());

        mustGit(work, "add", "first.testcase");
        mustGit(work, "commit", "-m", "first");
        mustGit(work, "push", "-u", "origin", "main");
    }

    private @NotNull String remoteUrl() {
        return remote.toUri().toString();
    }

    // UC-SHARE-016, Rule-SHARE-069, Rule-SHARE-120
    public void testSyncPushesABranchTheRemoteDoesNotHaveYet() {
        mustGit(work, "checkout", "-b", BRANCH);
        try {
            Files.writeString(work.resolve("second.testcase"), "{}");
        } catch (final IOException ex) {
            throw new AssertionError("Could not write the test case: " + ex.getMessage(), ex);
        }
        mustGit(work, "add", "second.testcase");
        mustGit(work, "commit", "-m", "second");

        final @NotNull GitCommitAndPush commits = new GitCommitAndPush(getProject());
        commits.pullWhereTheRemoteHasBranch(work, "origin", remoteUrl(), BRANCH);
        commits.push(work, "origin", remoteUrl(), BRANCH);

        assertEquals(mustGit(work, "rev-parse", "HEAD").trim(), mustGit(remote, "rev-parse", BRANCH).trim());
    }
}
