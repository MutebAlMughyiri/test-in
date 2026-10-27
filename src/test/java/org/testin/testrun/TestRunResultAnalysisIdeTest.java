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

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.TestRunResultAnalysisButton;
import org.testin.indexer.Nodes;
import org.testin.model.TestCase;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;
import org.testin.view.KeyPress;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestRunResultAnalysisIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull Transferable aScreenshot() {
        final @NotNull BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        return new Transferable() {
            @Override
            public DataFlavor @NotNull [] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(final @NotNull DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(final @NotNull DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor) ? image : "";
            }
        };
    }

    @Override
    protected void setUp() {
        super.setUp();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), TestRunResultAnalysisDialog.class);
        super.tearDown();
    }

    private @NotNull TestRunFixture aJudgedTestRunIn(final @NotNull TestRunStatus testRunStatus) {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, testCases.stream().map(testCase -> EditorFixtures.pending(testCase).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))).toList(), testCases);
        fixture.testRun().getMarker().changeStatus(testRunStatus);
        return fixture;
    }

    // Rule-EDITOR-PANEL-189
    public void testTheAnalysisCanBeWrittenOnlyOnceTheTestRunIsCompleted() {
        for (final TestRunStatus testRunStatus : List.of(TestRunStatus.CREATED, TestRunStatus.IN_PROGRESS, TestRunStatus.COMPLETED)) {
            final @NotNull TestRunFixture fixture = aJudgedTestRunIn(testRunStatus);
            final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
            try {
                final @NotNull TestRunResultAnalysisButton analysis = editor.getToolBar().getToolbarItem(TestRunResultAnalysisButton.class);
                assertEquals("Result Analysis on a " + testRunStatus.getLabel() + " test run", testRunStatus == TestRunStatus.COMPLETED, analysis.isEnabled());
            } finally {
                Disposer.dispose(editor);
            }
        }
    }

    // Rule-EDITOR-PANEL-192, Rule-INTERNAL-097
    public void testEnterMakesAParagraphAPictureIsNotPastedAndSaveIsAButton() {
        final @NotNull TestRunFixture fixture = aJudgedTestRunIn(TestRunStatus.COMPLETED);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull List<EditorTextField> realized = new ArrayList<>();
        try {
            editor.getToolBar().getToolbarItem(TestRunResultAnalysisButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JComponent dialog = ShownDialog.content(getProject(), TestRunResultAnalysisDialog.class);
            final @NotNull EditorTextField box = Drawn.first(dialog, EditorTextField.class);
            box.addNotify();
            realized.add(box);
            final @NotNull Editor typing = Optional.ofNullable(box.getEditor(true)).orElseThrow(() -> new AssertionError("a box of the analysis has no editor"));

            box.setText("Most failures came from the payment gateway");
            typing.getCaretModel().moveToOffset(typing.getDocument().getTextLength());
            assertTrue("the dialog took Enter from a box", KeyPress.answering(box, Shortcuts.Enter.getKey()).isEmpty());
            EditorTestUtil.executeAction(typing, IdeActions.ACTION_EDITOR_ENTER);
            assertTrue("Enter in a box did not make a new paragraph: " + box.getText(), box.getText().contains("\n"));
            assertTrue("Enter in a box closed the dialog", ShownDialog.isOpen(getProject(), TestRunResultAnalysisDialog.class));

            final @NotNull String typed = box.getText();
            CopyPasteManager.getInstance().setContents(aScreenshot());
            EditorTestUtil.executeAction(typing, IdeActions.ACTION_EDITOR_PASTE);
            assertEquals("a screenshot pasted something into a box", typed, box.getText());
            assertTrue("the analysis dialog keeps screenshots", Drawn.components(dialog).stream().noneMatch(component -> component.getClass().getSimpleName().startsWith("Screenshot")));

            final @NotNull JButton save = Drawn.components(dialog).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> "Save".equals(button.getText())).findFirst().orElseThrow(() -> new AssertionError("Save is not a button: " + Drawn.words(dialog)));
            save.doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("the Save button did not save the analysis", editor.getParent().getMarker().getResultAnalysis().values().stream().anyMatch(written -> written.startsWith("Most failures came from the payment gateway")));
        } finally {
            realized.forEach(EditorTextField::removeNotify);
            Disposer.dispose(editor);
        }
    }

    // UC-EDITOR-PANEL-045, Rule-TREE-PANEL-135
    public void testAnAnalysisSavedAfterTheTestRunWasCommittedIsRefusedAndNotWritten() {
        final @NotNull TestRunFixture fixture = aJudgedTestRunIn(TestRunStatus.COMPLETED);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.getToolBar().getToolbarItem(TestRunResultAnalysisButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JComponent dialog = ShownDialog.content(getProject(), TestRunResultAnalysisDialog.class);
            Drawn.first(dialog, EditorTextField.class).setText("Most failures came from the payment gateway");
            fixture.testRun().getMarker().changeStatus(TestRunStatus.COMMITTED);
            final @NotNull String why = fixture.testRun().whySignedOff().orElseThrow(() -> new AssertionError("a Committed test run gives no reason it is signed off"));
            final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

            Drawn.components(dialog).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> "Save".equals(button.getText())).findFirst().orElseThrow(() -> new AssertionError("Save is not a button: " + Drawn.words(dialog))).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("an analysis saved on a Committed test run was not refused with its reason", List.of(why), said.shown());
            assertTrue("an analysis was written onto a Committed test run", fixture.testRun().getMarker().getResultAnalysis().isEmpty());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // UC-EDITOR-PANEL-045
    public void testAnAnalysisSavedAfterTheTestRunWasRemovedSaysSoAndSaysNotSaved() {
        final @NotNull TestRunFixture fixture = aJudgedTestRunIn(TestRunStatus.COMPLETED);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.getToolBar().getToolbarItem(TestRunResultAnalysisButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JComponent dialog = ShownDialog.content(getProject(), TestRunResultAnalysisDialog.class);
            Drawn.first(dialog, EditorTextField.class).setText("Most failures came from the payment gateway");
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            nodes.removeTestRun(fixture.testRun().getPath(), _ -> {
            });
            Await.until("the test run was never removed", () -> !nodes.nodeExists(fixture.testRun().getPath()));
            final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

            Drawn.components(dialog).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> "Save".equals(button.getText())).findFirst().orElseThrow(() -> new AssertionError("Save is not a button: " + Drawn.words(dialog))).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("an analysis saved after its test run was removed did not say so", List.of(Bundle.message("test.run.gone", fixture.testRun().getName())), said.shown());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
