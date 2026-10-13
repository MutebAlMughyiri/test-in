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

package org.testin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.clipboard.CopyTestCaseValueAction;
import org.testin.clipboard.CutTestCaseAction;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.navigate.NavigateToTestCaseAction;
import org.testin.testcase.RemoveTestCaseAction;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.util.Bundle;
import org.testin.view.ViewDetailsAction;

import java.util.List;

public class GrayUnlessTestCaseSelectedIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull List<AnAction> theSevenActions() {
        return List.of(new CopyTestCaseAction(), new CopyTestCaseValueAction(), new CutTestCaseAction(), new NavigateToTestCaseAction(), new RemoveTestCaseAction(), new UpdateTestCaseAction(), new ViewDetailsAction());
    }

    private @NotNull AnActionEvent updated(final @NotNull AnAction action, final @NotNull TestSetEditor editor, final @NotNull List<TestCase> selected) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.EDITOR, editor)
                .add(TestinDataKeys.SELECTED_TEST_CASES, selected)
                .build());
        ActionUtil.updateAction(action, e);
        return e;
    }

    // Rule-EDITOR-PANEL-230
    public void testEveryTestCaseActionIsGrayAndSaysSoUntilATestCaseIsSelected() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Login");
        final @NotNull TestCase testCase = EditorFixtures.testCase(getProject(), testSet, "Log in with a valid user", "a");
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());

        for (final AnAction action : theSevenActions()) {
            final @NotNull String name = action.getClass().getSimpleName();
            final @NotNull AnActionEvent nothing = updated(action, editor, List.of());
            assertFalse(name + " works with no test case selected", nothing.getPresentation().isEnabled());
            assertEquals(name + " does not say why it is gray", Bundle.message("action.select.case.description"), nothing.getPresentation().getDescription());

            assertTrue(name + " stays gray with a test case selected", updated(action, editor, List.of(testCase)).getPresentation().isEnabled());
        }
    }

    // Rule-EDITOR-PANEL-230
    public void testUpdateStillNeedsAnEditor() {
        final @NotNull UpdateTestCaseAction update = new UpdateTestCaseAction();
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(update, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, List.of(new TestCase()))
                .build());
        ActionUtil.updateAction(update, e);

        assertFalse("Update works with no editor to update the test case in", e.getPresentation().isEnabled());
        assertEquals("Update does not say why it is gray", Bundle.message("action.select.case.description"), e.getPresentation().getDescription());
    }
}
