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

package org.testin.bug;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.indexer.TestCaseFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.testin.git.LocalGit.mustGit;

public class TestCaseLinkIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull Path IN_THE_TEST_PROJECT = Path.of("Test Cases", "ts2", "07f7e754-b849-4b38-9e6e-a2cacd84e927.testcase");

    private @NotNull Path aRepositoryOnRelease() {
        final @NotNull Path testProject = root.resolve("test-03");
        try {
            Files.createDirectories(testProject.resolve(IN_THE_TEST_PROJECT).getParent());
        } catch (final IOException ex) {
            throw new AssertionError("could not make the test project: " + ex.getMessage(), ex);
        }
        mustGit(testProject, "init", "--initial-branch=release");
        return testProject;
    }

    private @NotNull Optional<String> linkRead(final @NotNull Path testProject) {
        final @NotNull CompletableFuture<Optional<String>> read = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> read.complete(TestCaseLink.read(getProject(), new TestCaseFile(testProject, IN_THE_TEST_PROJECT))));
        Await.until("the link was never read", read::isDone);
        return read.join();
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-068
    public void testATestCaseInARepositoryWithARemoteLinksToItsFileOnTheBranchCheckedOut() {
        final @NotNull Path testProject = aRepositoryOnRelease();
        mustGit(testProject, "remote", "add", "origin", "https://github.com/mtb550/test-03.git");

        assertEquals("the bug does not link to the test case's file on the branch checked out", Optional.of("https://github.com/mtb550/test-03/blob/release/Test%20Cases/ts2/07f7e754-b849-4b38-9e6e-a2cacd84e927.testcase"), linkRead(testProject));
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-068
    public void testATestProjectWithNoRemoteHasNoLink() {
        assertEquals("a test project with no remote was given a link to its test case", Optional.empty(), linkRead(aRepositoryOnRelease()));
    }
}
