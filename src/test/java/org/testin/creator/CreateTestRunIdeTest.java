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

package org.testin.creator;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.NodesOnDisk;
import org.testin.TreeGesture;
import org.testin.creator.dialogs.CreateTestRunDialog;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;

public class CreateTestRunIdeTest extends AbstractOpenEditorsIdeTest {

    @Override
    public void tearDown() {
        ShownDialog.close(getProject(), CreateTestRunDialog.class);
        super.tearDown();
    }

    // UC-TREE-PANEL-010, Rule-TREE-PANEL-032, Rule-TREE-PANEL-033
    public void testUnderTestRunsTheSecondChoiceIsATestRunPackageAndNothingOpens() {
        final @NotNull TestProjectNode testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));

        TreeGesture.pressed(getProject(), new CreateTreeNodeAction(), List.of(testProject.getTestRunsFolder()));
        ShownDialog.typed(getProject(), CreateTestRunDialog.class, "Sprint 7");
        ShownDialog.press(getProject(), CreateTestRunDialog.class, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        ShownDialog.press(getProject(), CreateTestRunDialog.class, Shortcuts.Enter);

        final @NotNull Optional<AbstractNode> created = Services.getInstance(getProject(), Nodes.class).find(testProject.getTestRunsFolder().getPath().resolve("Sprint 7"));
        assertTrue("the second choice under Test Runs did not create a test run package", created.filter(TestRunPackageNode.class::isInstance).isPresent());
        assertEquals("a new test run package opened something", List.of(), openOn(created.orElseThrow()));
    }
}
