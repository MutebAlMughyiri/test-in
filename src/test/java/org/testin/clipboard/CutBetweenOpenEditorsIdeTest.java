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

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.editor.EditorFixtures;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import javax.swing.JComponent;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.stream.IntStream;

public class CutBetweenOpenEditorsIdeTest extends AbstractOpenEditorsIdeTest {

    private static @NotNull List<String> shownIn(final @NotNull TestSetEditor editor) {
        final @NotNull JBList<TestCase> list = editor.getList();
        return IntStream.range(0, list.getModel().getSize()).mapToObj(index -> list.getModel().getElementAt(index).getDescription()).toList();
    }

    @Override
    public void setUp() {
        super.setUp();
        CutState.startClipboardWatch(getProject());
    }

    @Override
    public void tearDown() {
        Services.getInstance(getProject(), CutState.class).clear();
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    private @NotNull TestSetEditor openedWith(final @NotNull TestProjectNode testProject, final @NotNull String name, final @NotNull List<String> descriptions) {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, name);
        for (int i = 0; i < descriptions.size(); i++) EditorFixtures.testCase(getProject(), testSet, descriptions.get(i), String.format("m%04d", i));
        opened(testSet);

        final @NotNull TestSetEditor editor = Services.getInstance(getProject(), TestinEditors.class).editorFor(testSet).filter(TestSetEditor.class::isInstance).map(TestSetEditor.class::cast).orElseThrow(() -> new AssertionError(name + " did not open in a test set editor"));
        Await.until(name + " never loaded", () -> !editor.isLoading());
        return editor;
    }

    private void choose(final @NotNull String actionId, final @NotNull JComponent on) {
        final @NotNull AnAction action = ActionManager.getInstance().getAction(actionId);
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, Gestures.dataOf(getProject(), on));
        ActionUtil.updateAction(action, e);
        assertTrue(actionId + " is gray: " + e.getPresentation().getDescription(), e.getPresentation().isEnabled());
        ActionUtil.performAction(action, e);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    // UC-EDITOR-PANEL-017
    public void testACutPastedIntoAnotherOpenEditorLeavesTheOpenEditorItCameFrom() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetEditor login = openedWith(testProject, "Login", List.of("Log in", "Log out"));
        final @NotNull TestSetEditor checkout = openedWith(testProject, "Checkout", List.of("Pay"));
        login.getList().setSelectedIndex(0);

        choose("Testin.CutTestCaseNode", login.getList());
        choose("Testin.PasteTestCaseNode", checkout.getList());

        Await.until("the pasted cut did not arrive in the editor it was pasted into: " + shownIn(checkout), () -> shownIn(checkout).equals(List.of("Pay", "Log in")));
        Await.until("the cut card is still shown in the editor it was cut from: " + shownIn(login), () -> shownIn(login).equals(List.of("Log out")));
    }
}
