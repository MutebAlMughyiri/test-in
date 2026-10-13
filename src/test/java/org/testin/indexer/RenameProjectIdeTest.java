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

package org.testin.indexer;

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.ProjectStatus;
import org.testin.services.Services;
import org.testin.util.JsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenameProjectIdeTest extends AbstractTempRootIdeTest {

    private @NotNull ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestProjectNode aTestProject(final String name) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectNode testProject = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(root.resolve(name));
            nodes().addTestProject(testProject);
            return testProject;
        });
    }

    private @NotNull TestSetNode aTestSetIn(final TestProjectNode testProject) {
        return WriteAction.computeAndWait(() -> {
            final TestSetNode testSet = Services.getInstance(getProject(), NodeMapper.class)
                    .getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes().addTestSet(testSet);
            return testSet;
        });
    }

    private void rename(final Path from, final Path to) {
        final AtomicBoolean done = new AtomicBoolean();
        nodes().renameNode(from, to, () -> done.set(true), () -> fail("the rename reported a failure"));

        Await.until("the rename never finished", done::get);
    }

    public void testARenamedProjectIsFoundUnderItsNewName() {
        final Path from = aTestProject("NAFATH").getPath();
        indexer().scanSingleProject(from);

        final Path to = root.resolve("Nafath_App");
        rename(from, to);

        assertTrue("the folder did not move", Files.isDirectory(to));
        assertTrue("the index does not hold the project under its new name", nodes().nodeExists(to));
        assertFalse("the index still holds the old name", nodes().nodeExists(from));
        assertTrue("its test cases folder did not follow", nodes().nodeExists(to.resolve("Test Cases")));
    }

    public void testAnInactiveProjectsContainersFollowIt() {
        final TestProjectNode testProject = aTestProject("Checkout");
        WriteAction.runAndWait(() -> {
            testProject.getMarker().setStatus(ProjectStatus.INACTIVE);
            nodes().persistMarker(testProject);
        });
        indexer().scanSingleProject(testProject.getPath());

        final Path to = root.resolve("Checkout_App");
        rename(testProject.getPath(), to);

        final TestProjectNode renamed = Optional.ofNullable(nodes().getTestProjectsByPath().get(to.toString()))
                .orElseThrow(() -> new AssertionError("the inactive project is not held under its new name"));
        assertEquals("its test cases folder kept the old path", to.resolve("Test Cases"), renamed.getTestCasesFolder().getPath());
        assertEquals("its test runs folder kept the old path", to.resolve("Test Runs"), renamed.getTestRunsFolder().getPath());
    }

    public void testAHandNamedTestCaseKeepsItsFile() {
        final TestProjectNode testProject = aTestProject("NAFATH");
        final TestSetNode testSet = aTestSetIn(testProject);
        final UUID testCaseId = UUID.randomUUID();

        try {
            Files.writeString(testSet.getPath().resolve("login.tc"), Services.getInstance(getProject(), JsonCodec.class)
                    .writeValueAsString(TestCase.builder().id(testCaseId).description("Log in with a valid user").build()));
        } catch (final Exception ex) {
            throw new AssertionError("Could not write the hand-named test case: " + ex.getMessage(), ex);
        }
        indexer().scanSingleProject(testProject.getPath());

        final Path to = root.resolve("Nafath_App");
        rename(testProject.getPath(), to);

        final TestCase testCase = indexedTestCases().findTestCase(testCaseId).orElseThrow();
        final TestCaseFile file = indexedTestCases().testCaseFile(testCase).orElseThrow();

        assertEquals("the test case is placed in the renamed project", to, file.testProject());
        assertEquals("the test case lost its hand-named file", "login.tc", file.inProject().getFileName().toString());
    }

    // Rule-TREE-PANEL-004
    public void testANameIsTakenOnDiskButNotByTheNodeItself() {
        final Path other = root.resolve("Payments");
        final Path self = root.resolve("nafath");
        try {
            Files.createDirectories(other);
            Files.createDirectories(self);
        } catch (final Exception ex) {
            throw new AssertionError("Could not make the folders: " + ex.getMessage(), ex);
        }

        assertTrue("a sibling folder the index never read was not seen", nodes().isTaken(other, Optional.empty()));
        assertTrue("a sibling is in the way of a rename", nodes().isTaken(other, Optional.of(self)));
        assertFalse("a free name was reported taken", nodes().isTaken(root.resolve("Checkout"), Optional.empty()));
        assertFalse("changing only the case of a name was refused as taken", nodes().isTaken(root.resolve("Nafath"), Optional.of(self)));
    }
}
