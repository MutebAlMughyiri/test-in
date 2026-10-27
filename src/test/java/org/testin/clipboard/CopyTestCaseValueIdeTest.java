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
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.UiInterceptors;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Gestures;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CopyTestCaseValueIdeTest extends AbstractTempRootIdeTest {

    @Override
    protected void tearDown() {
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    // UC-EDITOR-PANEL-014, Rule-EDITOR-PANEL-207
    public void testCopyTestCaseValueOffersItsMenuAndEnterCopiesAllDetails() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        final @NotNull TestCase testCase = EditorFixtures.testCases(getProject(), testSet, 2).getFirst();
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull AnAction copy = Objects.requireNonNull(ActionManager.getInstance().getAction("Testin.CopyTestCase"), "Copy Test Case Value is not registered");
        try {
            editor.getList().setSelectedIndex(0);
            ActionUtil.performAction(copy, TestActionEvent.createTestEvent(copy, Gestures.dataOf(getProject(), editor.getList())));
            assertEquals("Copy Test Case Value did not offer its menu", 1, shown.size());

            ShownDialog.press(shown.getFirst().getContent(), Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("Enter did not copy every detail of the selected test case", CopyChoice.ALL_DETAILS.from(List.of(testCase)), CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
            assertEquals("the copy was not confirmed once, naming what was copied", List.of(CopyChoice.ALL_DETAILS.copiedMessage(1)), said.shown());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
