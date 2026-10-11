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

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.OnScreen;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.TestRunResultAnalysisBtn;
import org.testin.model.TestCaseDto;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.Execution;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

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

    private @NotNull TestRunFixture aJudgedTestRunIn(final @NotNull TestRunStatus status) {
        final @NotNull List<TestCaseDto> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, testCases.stream().map(tc -> EditorFixtures.pending(tc).setExecutions(List.of(Execution.builder().status(RunItemStatus.PASSED).build()))).toList(), testCases);
        fixture.testRun().getMarker().changeStatus(status);
        return fixture;
    }

    // Rule-EDITOR-PANEL-189
    public void testTheAnalysisCanBeWrittenOnlyOnceTheTestRunIsCompleted() {
        for (final TestRunStatus status : List.of(TestRunStatus.CREATED, TestRunStatus.IN_PROGRESS, TestRunStatus.COMPLETED)) {
            final @NotNull TestRunFixture fixture = aJudgedTestRunIn(status);
            final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
            try {
                final @NotNull TestRunResultAnalysisBtn analysis = editor.getToolBar().getToolbarItem(TestRunResultAnalysisBtn.class);
                assertEquals("Result Analysis on a " + status.getLabel() + " test run", status == TestRunStatus.COMPLETED, analysis.isEnabled());
            } finally {
                Disposer.dispose(editor);
            }
        }
    }

    // Rule-EDITOR-PANEL-192
    public void testEnterMakesAParagraphAPictureIsNotPastedAndSaveIsAButton() {
        final @NotNull TestRunFixture fixture = aJudgedTestRunIn(TestRunStatus.COMPLETED);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.getToolBar().getToolbarItem(TestRunResultAnalysisBtn.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JComponent dialog = ShownDialog.content(getProject(), TestRunResultAnalysisDialog.class);
            final @NotNull JBTextArea box = Drawn.components(dialog).stream().filter(JBTextArea.class::isInstance).map(JBTextArea.class::cast).findFirst().orElseThrow();

            box.setText("Most failures came from the payment gateway");
            box.setCaretPosition(box.getText().length());
            assertTrue("Enter is not bound in a box", OnScreen.pressKey(box, Shortcuts.Enter.getKey()));
            assertTrue("Enter in a box did not make a new paragraph: " + box.getText(), box.getText().contains("\n"));
            assertTrue("Enter in a box closed the dialog", ShownDialog.isOpen(getProject(), TestRunResultAnalysisDialog.class));

            final @NotNull String typed = box.getText();
            assertEquals("Ctrl+V in a box is not the box's own paste", DefaultEditorKit.pasteAction, box.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_V, Shortcuts.menuMask())));
            assertFalse("a box took a screenshot", box.getTransferHandler().importData(new TransferHandler.TransferSupport(box, aScreenshot())));
            assertEquals("a screenshot pasted something into a box", typed, box.getText());
            assertTrue("the analysis dialog keeps screenshots", Drawn.components(dialog).stream().noneMatch(component -> component.getClass().getSimpleName().startsWith("Screenshot")));

            final @NotNull JButton save = Drawn.components(dialog).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                    .filter(button -> "Save".equals(button.getText())).findFirst().orElseThrow(() -> new AssertionError("Save is not a button: " + Drawn.words(dialog)));
            save.doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("the Save button did not save the analysis", editor.getParent().getMarker().getResultAnalysis().values().stream().anyMatch(written -> written.startsWith("Most failures came from the payment gateway")));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
