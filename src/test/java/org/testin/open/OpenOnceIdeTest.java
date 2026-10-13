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

package org.testin.open;

import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TreeGesture;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;

import java.util.Arrays;
import java.util.List;

public class OpenOnceIdeTest extends AbstractOpenEditorsIdeTest {

    // Rule-TREE-PANEL-023
    public void testOpeningAnOpenTestSetBringsItsTabForward() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = made.testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestSetNode checkout = made.testSet(testProject.getTestCasesFolder(), "Checkout");

        TreeGesture.pressed(getProject(), new OpenAction(), List.of(login));
        Await.until("Open did not open the test set", () -> openOn(login).size() == 1);
        TreeGesture.pressed(getProject(), new OpenAction(), List.of(checkout));
        Await.until("Open did not open the second test set", () -> openOn(checkout).size() == 1);

        TreeGesture.pressed(getProject(), new OpenAction(), List.of(login));

        Await.until("opening the open test set did not bring its tab forward", () -> Arrays.asList(FileEditorManager.getInstance(getProject()).getSelectedFiles()).equals(openOn(login)));
        assertEquals("opening the open test set opened it a second time", 1, openOn(login).size());
    }
}
