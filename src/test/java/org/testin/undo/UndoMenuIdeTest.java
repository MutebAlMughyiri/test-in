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

package org.testin.undo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;
import org.testin.view.Drawn;

import javax.swing.text.JTextComponent;
import java.util.Objects;

public class UndoMenuIdeTest extends AbstractTempRootIdeTest {

    private @NotNull String testerBefore = "";

    private static @NotNull AnAction entry(final @NotNull TestSetEditor editor, final @NotNull UndoDirection direction) {
        return ActionUtil.getActions(editor.getList()).stream()
                .filter(TestinUndoAction.class::isInstance)
                .filter(action -> direction.getTitle().equals(action.getTemplatePresentation().getText()))
                .findFirst().orElseThrow(() -> new AssertionError("the editor's menu has no " + direction.getTitle()));
    }

    private static @NotNull AnActionEvent updated(final @NotNull AnAction action) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action);
        ActionUtil.updateAction(action, e);
        return e;
    }

    private static @NotNull String said(final @NotNull AnAction action) {
        return Objects.toString(updated(action).getPresentation().getText(), "");
    }

    private static void typeSignInAsTheDescription(final @NotNull TestSetEditor editor) {
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JBTable grid = Drawn.components(editor.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow();

        assertTrue(grid.editCellAt(0, grid.convertColumnIndexToView(TestSetEditorFields.DESCRIPTION.column())));
        ((JTextComponent) grid.getEditorComponent()).setText("Sign in");
        assertTrue(grid.getCellEditor().stopCellEditing());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    @Override
    protected void setUp() {
        super.setUp();
        testerBefore = Services.getInstance(AppSettingsState.class).testerName;
        Services.getInstance(AppSettingsState.class).testerName = "Sara";
    }

    @Override
    protected void tearDown() {
        Services.getInstance(AppSettingsState.class).testerName = testerBefore;
        super.tearDown();
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    // Rule-EDITOR-PANEL-067, Rule-EDITOR-PANEL-071, Rule-EDITOR-PANEL-068
    public void testTheEntriesSayWhatTheNextPressWouldTakeOrPutBackAndUndoIsExact() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        final @NotNull TestCaseDto testCase = EditorFixtures.testCase(getProject(), testSet, "Log in", "m");
        final @NotNull JsonCodec mapper = Services.getInstance(getProject(), JsonCodec.class);
        final @NotNull String original = mapper.writeValueAsString(theTestCases().findTestCase(testCase.getId()).orElseThrow());
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull AnAction undo = entry(editor, UndoDirection.UNDO);
        final @NotNull AnAction redo = entry(editor, UndoDirection.REDO);
        final @NotNull String named = Bundle.message("snapshot.undo.one", Bundle.message("snapshot.verb.edit"), "Sign in");

        typeSignInAsTheDescription(editor);
        Await.until("the edit never reached the undo history", () -> updated(undo).getPresentation().isEnabled());

        assertEquals("Undo does not say what it would take back", UndoDirection.UNDO.getTitle() + " " + named, said(undo));
        assertEquals("the tester who changed it is not recorded", "Sara", theTestCases().findTestCase(testCase.getId()).orElseThrow().getUpdatedBy());

        ActionUtil.performAction(undo, updated(undo));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("undo did not put the test case back exactly, with who last changed it and when", original, mapper.writeValueAsString(theTestCases().findTestCase(testCase.getId()).orElseThrow()));
        assertEquals("Redo does not say what it would put back", UndoDirection.REDO.getTitle() + " " + named, said(redo));
        assertEquals("Undo names something with nothing left to take back", UndoDirection.UNDO.getTitle(), said(undo));
    }
}
