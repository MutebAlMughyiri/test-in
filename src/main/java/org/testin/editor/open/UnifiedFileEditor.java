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

package org.testin.editor.open;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;
import java.util.List;

@AllArgsConstructor
public class UnifiedFileEditor extends UserDataHolderBase implements FileEditor {
    private final @NotNull Project project;

    private final @NotNull UnifiedVirtualFile file;

    @Getter
    private final @NotNull TestinEditor editor;

    @Override
    public @NotNull JComponent getComponent() {
        return editor.getComponent();
    }

    @Override
    public @NotNull JComponent getPreferredFocusedComponent() {
        return editor.getPreferredFocusedComponent();
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-011
    @Override
    public @NotNull String getName() {
        return file.getNode().getName();
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    // Rule-EDITOR-PANEL-297
    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void dispose() {
        editor.dispose();
    }

    @Override
    public void addPropertyChangeListener(final @NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(final @NotNull PropertyChangeListener listener) {
    }

    @Override
    public void setState(final @NotNull FileEditorState state) {
    }

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-113
    @Override
    public void selectNotify() {
        final @NotNull List<TestCase> selected = editor.getSelectedTestCases();

        ViewToolWindowFactory.panel(project).ifPresent(viewer -> {
            if (selected.isEmpty()) {
                viewer.reset();
                return;
            }

            viewer.showIfOpen(selected, file.getNode().treePath());
        });
    }
}
