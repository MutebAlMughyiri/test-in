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
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TreeGesture;
import org.testin.creator.dialogs.CreateNodeDialog;
import org.testin.indexer.Nodes;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;

public class CreateTestRunIdeTest extends AbstractOpenEditorsIdeTest {

    @Override
    public void tearDown() {
        ShownDialog.close(getProject(), CreateNodeDialog.class);
        super.tearDown();
    }

    // UC-TREE-PANEL-010, Rule-TREE-PANEL-032, Rule-TREE-PANEL-033
    public void testUnderTestRunsTheSecondChoiceIsATestRunPackageAndNothingOpens() {
        final @NotNull TestProjectNode testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));

        TreeGesture.pressed(getProject(), new CreateTreeNodeAction(), List.of(testProject.getTestRunsFolder()));
        ShownDialog.typed(getProject(), CreateNodeDialog.class, "Sprint 7");
        ShownDialog.press(getProject(), CreateNodeDialog.class, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        ShownDialog.press(getProject(), CreateNodeDialog.class, Shortcuts.Enter);

        final @NotNull Path wanted = testProject.getTestRunsFolder().getPath().resolve("Sprint 7");
        Await.until("the second choice under Test Runs did not create a test run package", () -> Services.getInstance(getProject(), Nodes.class).find(wanted).filter(TestRunPackageNode.class::isInstance).isPresent());
        assertEquals("a new test run package opened something", List.of(), openOn(Services.getInstance(getProject(), Nodes.class).find(wanted).orElseThrow()));
    }
}
