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

package org.testin.explorer.tree;

import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.TestProjectStatus;

import java.nio.file.Path;
import java.util.Objects;

import static org.testng.Assert.assertNotEquals;

public class TreePanelNodeIdeTest extends AbstractIdeTest {

    private static final Path SET = Path.of("project", "Test Cases", "Login");

    private static @NotNull AbstractNode at(final AbstractNode directory, final Path path) {
        directory.setPath(path);
        return directory;
    }

    public void testARescanReadsTheSameFolderIntoAnEqualNode() {
        final TreePanelNode before = new TreePanelNode(getProject(), at(new TestSetNode(), SET));
        final TreePanelNode after = new TreePanelNode(getProject(), at(new TestSetNode(), SET));

        assertEquals("a rescan builds a new DTO for the same folder, and the tree must see the same node", before, after);
        assertEquals(before.hashCode(), after.hashCode());
    }

    public void testTwoFoldersAreTwoNodes() {
        final TreePanelNode login = new TreePanelNode(getProject(), at(new TestSetNode(), SET));
        final TreePanelNode checkout = new TreePanelNode(getProject(), at(new TestSetNode(), SET.resolveSibling("Checkout")));

        assertNotEquals(login, checkout);
    }

    // Rule-TREE-PANEL-127
    public void testATestSetAndATestRunHaveNothingToOpen() {
        assertEquals(LeafState.ALWAYS, new TreePanelNode(getProject(), at(new TestSetNode(), SET)).getLeafState());
        assertEquals(LeafState.ALWAYS, new TreePanelNode(getProject(), at(new TestRunNode(), Path.of("project", "Test Runs", "Cycle 1"))).getLeafState());
    }

    public void testAFolderCanBeFoundByItsPathAlone() {
        final Path root = Path.of("project");
        final TreePanelNode probe = TreePanelNode.standingFor(getProject(), root, SET);

        assertEquals("the model finds a changed folder by comparing a probe with the node it holds",
                new TreePanelNode(getProject(), at(new TestSetNode(), SET)), probe);
        assertEquals(new TreePanelNode(getProject(), at(new TestSetPackageNode(), Objects.requireNonNull(SET.getParent(), "the test set has no folder"))), probe.getParent());
        assertEquals("the probe's parents lead to the root the structure answers with",
                new TreePanelNode(getProject(), at(new TestProjectNode(), root)), probe.getParent().getParent());
    }

    public void testAPackageIsAskedForItsChildren() {
        assertEquals(LeafState.ASYNC, new TreePanelNode(getProject(), at(new TestSetPackageNode(), Objects.requireNonNull(SET.getParent(), "the test set has no folder"))).getLeafState());
    }

    // Rule-TREE-PANEL-063
    public void testATestProjectThatIsNotActiveShowsNothingUnderIt() {
        final @NotNull TestProjectNode testProject = new TestProjectNode();
        testProject.setPath(Path.of("project"));

        assertEquals("an active test project does not show its two containers", 2, new TreePanelNode(getProject(), testProject).getChildren().size());

        testProject.getMarker().setStatus(TestProjectStatus.INACTIVE);
        assertTrue("an inactive test project shows what it holds", new TreePanelNode(getProject(), testProject).getChildren().isEmpty());
    }
}
