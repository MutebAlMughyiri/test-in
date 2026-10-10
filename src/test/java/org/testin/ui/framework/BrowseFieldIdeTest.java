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

package org.testin.ui.framework;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
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
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.AbstractTempRootIdeTest;
import org.testin.util.Bundle;

import javax.swing.JTextField;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrowseFieldIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull VirtualFile folder(final @NotNull Path path) {
        try {
            Files.createDirectories(path);
        } catch (final IOException ex) {
            throw new AssertionError("could not make " + path, ex);
        }
        return Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)).orElseThrow(() -> new AssertionError("the IDE does not see " + path));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-118
    public void testAFolderFieldIsTheFrameworksFieldWithABrowseButtonThatOpensTheChooserWhereTheFieldPointsAndPutsTheChoiceBack() {
        final @NotNull VirtualFile typed = folder(root.resolve("Downloads"));
        final @NotNull VirtualFile chosen = folder(root.resolve("Reports"));
        final @NotNull Chooser chooser = new Chooser(chosen);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, chooser, getTestRootDisposable());

        final @NotNull TextInput input = ComponentDialogBase.textField().caption("Folder").value(typed.getPresentableUrl())
                .browse(getProject(), FileChooserDescriptorFactory.createSingleFolderDescriptor()).build().getComponent();
        final @NotNull JTextField field = (JTextField) input.getFocusComponent();

        assertTrue("a folder field is not the framework's text field", field instanceof ExtendableTextField);
        final @NotNull ExtendableTextComponent.Extension browse = ((ExtendableTextField) field).getExtensions().stream()
                .filter(extension -> Bundle.message("dialog.browse").equals(extension.getTooltip())).findFirst()
                .orElseThrow(() -> new AssertionError("the folder field has no browse button inside it"));
        assertFalse("the browse button is not at the field's right edge", browse.isIconBeforeText());

        Optional.ofNullable(browse.getActionOnClick()).orElseThrow(() -> new AssertionError("the browse button does nothing")).run();

        assertEquals("the chooser did not open at the path the field holds", List.of(typed), chooser.openedAt);
        assertEquals("the chosen path was not put in the field", chosen.getPresentableUrl(), field.getText());
    }

    private static final class Chooser extends FileChooserFactory {
        private final @NotNull VirtualFile answer;
        private final @NotNull List<VirtualFile> openedAt = new ArrayList<>();

        private Chooser(final @NotNull VirtualFile answer) {
            this.answer = answer;
        }

        @Override
        public @NotNull FileChooserDialog createFileChooser(final @NotNull FileChooserDescriptor descriptor, final @Nullable Project p, final @Nullable Component parent) {
            return (_, toSelect) -> {
                openedAt.addAll(List.of(toSelect));
                return new VirtualFile[]{answer};
            };
        }

        @Override
        public @NotNull PathChooserDialog createPathChooser(final @NotNull FileChooserDescriptor descriptor, final @Nullable Project p, final @Nullable Component parent) {
            return (toSelect, callback) -> {
                if (toSelect != null) openedAt.add(toSelect);
                callback.consume(List.of(answer));
            };
        }

        @Override
        public @NotNull FileSaverDialog createSaveFileDialog(final @NotNull FileSaverDescriptor descriptor, final @Nullable Project p) {
            throw new UnsupportedOperationException("a folder field never saves");
        }

        @Override
        public @NotNull FileSaverDialog createSaveFileDialog(final @NotNull FileSaverDescriptor descriptor, final @NotNull Component parent) {
            throw new UnsupportedOperationException("a folder field never saves");
        }

        @Override
        public @NotNull FileTextField createFileTextField(final @NotNull FileChooserDescriptor descriptor, final boolean showHidden, final @Nullable Disposable parent) {
            throw new UnsupportedOperationException("a folder field builds no field of its own");
        }

        @Override
        public void installFileCompletion(final @NotNull JTextField field, final @NotNull FileChooserDescriptor descriptor, final boolean showHidden, final @Nullable Disposable parent) {
        }
    }
}
