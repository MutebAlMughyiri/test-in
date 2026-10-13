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

package org.testin.editor.testrun;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.KeyStroke;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutionByKeyboardIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull Set<KeyStroke> keysAnsweredBy(final @NotNull JBList<?> list) {
        return ActionUtil.getActions(list).stream()
                .flatMap(action -> Arrays.stream(action.getShortcutSet().getShortcuts()))
                .filter(KeyboardShortcut.class::isInstance)
                .map(shortcut -> ((KeyboardShortcut) shortcut).getFirstKeyStroke())
                .collect(Collectors.toSet());
    }

    // Rule-PRODUCT-024
    public void testEveryStepOfExecutingATestRunHasAKey() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode login = EditorFixtures.testSet(getProject(), testProject, "Login");
        final @NotNull List<TestCase> testCases = EditorFixtures.testCases(getProject(), login, 2);
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRun(getProject(), testProject, testCases.stream().map(EditorFixtures::pending).toList()), getTestRootDisposable());
        final @NotNull JBList<?> list = Drawn.components(editor.getComponent()).stream()
                .filter(JBList.class::isInstance)
                .map(component -> (JBList<?>) component)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the test run editor shows no list of test cases"));

        final @NotNull Set<KeyStroke> answered = keysAnsweredBy(list);
        for (final RunItemStatus runItemStatus : List.of(RunItemStatus.PASSED, RunItemStatus.FAILED, RunItemStatus.BLOCKED)) {
            assertTrue("recording " + runItemStatus + " needs the mouse: " + answered, answered.contains(runItemStatus.getMenuEntry().shortcut()));
        }
        assertTrue("the report needs the mouse: " + answered, answered.contains(Shortcuts.GenerateReport.getKey()));
        assertTrue("the menu on a test case needs the mouse: " + answered, answered.contains(Shortcuts.ContextMenu.getKey()));

        for (final String actionId : List.of("Testin.UpdateRunItem", "Testin.RunTestMethod", "Testin.NavigateToTestMethod")) {
            assertTrue(actionId + " has no key, so that step of the execution needs the mouse", KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId).length > 0);
        }
    }
}
