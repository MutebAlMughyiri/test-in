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

package org.testin.importexport.imports;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.TreeGesture;
import org.testin.importexport.FileFormat;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChooseAFileIdeTest extends AbstractTempRootIdeTest {
    private final @NotNull List<Consumer<? super List<VirtualFile>>> asked = new CopyOnWriteArrayList<>();

    private static @NotNull JBTabbedPane tabsIn(final @NotNull JComponent content) {
        return Drawn.first(content, JBTabbedPane.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, new WaitingChooser(asked), getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ImportDialog.class);
        super.tearDown();
    }

    private @NotNull VirtualFile aPlan(final @NotNull Map<String, List<TestCase>> sheets) {
        final @NotNull File plan = root.resolve("Plan.json").toFile();
        FileFormat.JSON.exportToFile(getProject(), plan, sheets);
        return Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(plan)).orElseThrow(() -> new AssertionError("the plan was not written: " + plan));
    }

    private @NotNull TestSetNode theLoginTestSet() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        return made.testSet(made.testProject(root.resolve("Demo")).getTestCasesFolder(), "Login");
    }

    private @NotNull JComponent openedOn(final @NotNull TestSetNode testSet) {
        TreeGesture.pressed(getProject(), ActionManager.getInstance().getAction("Testin.Import"), List.of(testSet));
        Await.until("the import did not ask for a file", () -> !asked.isEmpty());
        return ShownDialog.content(getProject(), ImportDialog.class);
    }

    private void chosen(final @NotNull JComponent dialog, final @NotNull VirtualFile plan, final int sheets) {
        asked.getFirst().consume(List.of(plan));
        Await.until("the chosen file was not shown", () -> tabsIn(dialog).getTabCount() == sheets);
    }

    // UC-SHARE-007, Rule-SHARE-035
    public void testTheTableIsEmptyUntilAFileIsChosen() {
        final @NotNull VirtualFile plan = aPlan(Map.of("Login", List.of(TestCase.builder().description("log in with a valid user").build())));
        final @NotNull JComponent dialog = openedOn(theLoginTestSet());

        assertEquals("the table showed something before a file was chosen", 0, tabsIn(dialog).getTabCount());

        chosen(dialog, plan, 1);
        assertEquals("Login", tabsIn(dialog).getTitleAt(0));
        assertTrue(UIUtil.uiTraverser(dialog).filter(JTextField.class).toList().stream().anyMatch(field -> field.getText().endsWith("Plan.json")));
    }

    // UC-SHARE-005, UC-SHARE-007
    public void testEnterImportsTheChosenFileIntoTheTestSetAndClosesTheDialog() {
        final @NotNull VirtualFile plan = aPlan(Map.of("Login", List.of(TestCase.builder().description("log in with a valid user").build())));
        final @NotNull TestSetNode login = theLoginTestSet();
        final @NotNull JComponent dialog = openedOn(login);
        chosen(dialog, plan, 1);
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        ShownDialog.press(getProject(), ImportDialog.class, Shortcuts.Enter);

        Await.until("the chosen file was not imported", () -> said.shown().contains(Done.counted(Done.IMPORTED.getOutcome(), 1)));
        assertFalse("the dialog stayed open after the import began", ShownDialog.isOpen(getProject(), ImportDialog.class));
        assertEquals("the test case imported is not in the test set", List.of("log in with a valid user"), Services.getInstance(getProject(), TestCases.class).getTestCasesForTestSet(login.getPath()).stream().map(TestCase::getDescription).toList());
    }

    // UC-SHARE-007
    public void testEnterBeforeAFileIsChosenImportsNothingAndAFileWithNothingInItSaysSo() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull JComponent dialog = openedOn(theLoginTestSet());

        ShownDialog.press(getProject(), ImportDialog.class, Shortcuts.Enter);
        assertTrue("Enter before a file was chosen closed the dialog", ShownDialog.isOpen(getProject(), ImportDialog.class));

        chosen(dialog, aPlan(Map.of()), 0);
        ShownDialog.press(getProject(), ImportDialog.class, Shortcuts.Enter);

        assertEquals("a file with nothing in it did not say so", List.of(Bundle.message("notification.import.empty.title") + "\n" + Bundle.message("notification.import.empty.nothing.loaded")), said.shown());
        assertTrue("the dialog closed over a file with nothing in it", ShownDialog.isOpen(getProject(), ImportDialog.class));
    }

    private static final class WaitingChooser extends FileChooserFactory {
        private final @NotNull List<Consumer<? super List<VirtualFile>>> asked;

        private WaitingChooser(final @NotNull List<Consumer<? super List<VirtualFile>>> asked) {
            this.asked = asked;
        }

        @Override
        public @NotNull FileChooserDialog createFileChooser(final @NotNull FileChooserDescriptor descriptor, final @Nullable Project project, final @Nullable Component parent) {
            throw new UnsupportedOperationException("the import asks through a path chooser");
        }

        @Override
        public @NotNull PathChooserDialog createPathChooser(final @NotNull FileChooserDescriptor descriptor, final @Nullable Project project, final @Nullable Component parent) {
            return (_, callback) -> asked.add(callback);
        }

        @Override
        public @NotNull FileSaverDialog createSaveFileDialog(final @NotNull FileSaverDescriptor descriptor, final @Nullable Project project) {
            throw new UnsupportedOperationException("nothing is saved here");
        }

        @Override
        public @NotNull FileSaverDialog createSaveFileDialog(final @NotNull FileSaverDescriptor descriptor, final @NotNull Component parent) {
            throw new UnsupportedOperationException("nothing is saved here");
        }

        @Override
        public @NotNull FileTextField createFileTextField(final @NotNull FileChooserDescriptor descriptor, final boolean showHidden, final @Nullable Disposable parent) {
            throw new UnsupportedOperationException("no file field is made here");
        }

        @Override
        public void installFileCompletion(final @NotNull JTextField field, final @NotNull FileChooserDescriptor descriptor, final boolean showHidden, final @Nullable Disposable parent) {
            throw new UnsupportedOperationException("no completion is installed here");
        }
    }
}
