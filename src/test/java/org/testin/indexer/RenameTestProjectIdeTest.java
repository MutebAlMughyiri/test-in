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
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.TestProjectStatus;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenameTestProjectIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestProjectIndexer indexer() {
        return Services.getInstance(getProject(), TestProjectIndexer.class);
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

    private void rename(final Path from, final Path to) {
        final AtomicBoolean done = new AtomicBoolean();
        nodes().renameNode(from, to, () -> done.set(true), () -> fail("the rename reported a failure"));

        Await.until("the rename never finished", done::get);
    }

    public void testARenamedTestProjectIsFoundUnderItsNewName() {
        final Path from = aTestProject("NAFATH").getPath();
        indexer().scanSingleTestProject(from);

        final Path to = root.resolve("Nafath_App");
        rename(from, to);

        assertTrue("the folder did not move", Files.isDirectory(to));
        assertTrue("the index does not hold the project under its new name", nodes().nodeExists(to));
        assertFalse("the index still holds the old name", nodes().nodeExists(from));
        assertTrue("its test cases folder did not follow", nodes().nodeExists(to.resolve("Test Cases")));
    }

    public void testAnInactiveTestProjectsContainersFollowIt() {
        final TestProjectNode testProject = aTestProject("Checkout");
        WriteAction.runAndWait(() -> {
            testProject.getMarker().setStatus(TestProjectStatus.INACTIVE);
            nodes().persistMarker(testProject);
        });
        indexer().scanSingleTestProject(testProject.getPath());

        final Path to = root.resolve("Checkout_App");
        rename(testProject.getPath(), to);

        final TestProjectNode renamed = Optional.ofNullable(nodes().getTestProjectsByPath().get(to.toString()))
                .orElseThrow(() -> new AssertionError("the inactive project is not held under its new name"));
        assertEquals("its test cases folder kept the old path", to.resolve("Test Cases"), renamed.getTestCasesFolder().getPath());
        assertEquals("its test runs folder kept the old path", to.resolve("Test Runs"), renamed.getTestRunsFolder().getPath());
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
