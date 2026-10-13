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

package org.testin.testrun;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.List;

public class TestRunStatusFollowsIdeTest extends AbstractOpenEditorsIdeTest {

    private TestRunNode testRun;

    private static @NotNull TestRunStatus inTheEditor(final @NotNull TestRunEditor editor) {
        return editor.shownTestRun().orElseThrow(() -> new AssertionError("the editor shows no test run")).getMarker().getStatus();
    }

    @Override
    public void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = made.testSet(testProject.getTestCasesFolder(), "Login");
        testRun = made.testRun(testProject.getTestRunsFolder(), "Cycle-1");
        Services.getInstance(getProject(), TestRuns.class).putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(List.of(new RunItem().setId(made.testCase(login).getId())))));
    }

    private @NotNull TestRunEditor theEditor() {
        final @NotNull TestRunEditor editor = Services.getInstance(getProject(), TestinEditors.class).testRunEditorFor(testRun).orElseThrow(() -> new AssertionError("the test run has no editor open"));
        Await.until("the test run editor never loaded", () -> editor.shownTestRun().isPresent());
        return editor;
    }

    private @NotNull TestRunStatus inTheTree() {
        final @NotNull AbstractNode row = Services.getInstance(getProject(), Nodes.class).find(testRun.getPath()).orElseThrow(() -> new AssertionError("the test run is not in the tree"));
        return ((TestRunNode) row).getMarker().getStatus();
    }

    // Rule-TREE-PANEL-091
    public void testAStatusSetFromTheTreeOrTheEditorIsFollowedByBoth() {
        opened(testRun);
        final @NotNull TestRunEditor editor = theEditor();
        final @NotNull TestRunNode treeRow = (TestRunNode) Services.getInstance(getProject(), Nodes.class).find(testRun.getPath()).orElseThrow();

        Services.getInstance(getProject(), TestRunStatusChange.class).apply(treeRow, TestRunStatus.IN_PROGRESS);

        Await.until("the open editor did not follow a status set from the tree", () -> inTheEditor(editor) == TestRunStatus.IN_PROGRESS);
        assertEquals("the tree row did not follow the status set on it", TestRunStatus.IN_PROGRESS, inTheTree());

        Services.getInstance(getProject(), TestRunStatusChange.class).apply(editor.shownTestRun().orElseThrow(), TestRunStatus.ASSIGNED);

        Await.until("the tree row did not follow a status set from the editor", () -> inTheTree() == TestRunStatus.ASSIGNED);
        assertEquals("the editor did not follow the status set on it", TestRunStatus.ASSIGNED, inTheEditor(editor));
    }
}
