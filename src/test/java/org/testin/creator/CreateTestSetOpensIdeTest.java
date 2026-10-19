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
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.nio.file.Path;
import java.util.List;

public class CreateTestSetOpensIdeTest extends AbstractOpenEditorsIdeTest {

    @Override
    public void tearDown() {
        ShownDialog.close(getProject(), CreateNodeDialog.class);
        super.tearDown();
    }

    // Rule-TREE-PANEL-026
    public void testANewTestSetOpensInItsEditorAtOnce() {
        final @NotNull TestProjectNode testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));

        TreeGesture.pressed(getProject(), new CreateTreeNodeAction(), List.of(testProject.getTestCasesFolder()));
        ShownDialog.typed(getProject(), CreateNodeDialog.class, "Login");
        ShownDialog.press(getProject(), CreateNodeDialog.class, Shortcuts.Enter);

        final @NotNull Path wanted = testProject.getTestCasesFolder().getPath().resolve("Login");
        Await.until("the test set was not created", () -> Services.getInstance(getProject(), Nodes.class).find(wanted).isPresent());
        final @NotNull AbstractNode created = Services.getInstance(getProject(), Nodes.class).find(wanted).orElseThrow();
        Await.until("the new test set did not open in its editor", () -> openOn(created).size() == 1);
    }
}
