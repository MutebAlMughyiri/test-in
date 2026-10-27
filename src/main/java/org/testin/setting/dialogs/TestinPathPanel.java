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

package org.testin.setting.dialogs;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.JButton;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public final class TestinPathPanel {
    private final @NotNull TextFieldWithBrowseButton pathField = new TextFieldWithBrowseButton();

    private final @NotNull JButton openFolderButton = new JButton(Bundle.message("settings.path.open"));

    private final @NotNull ExecutorService probe = AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Folder Probe", 1);

    private final @NotNull AtomicReference<String> asked = new AtomicReference<>("");

    public TestinPathPanel() {
        setupField();
        setupOpenButton();
        setupValidationListener();
    }

    private static boolean isFolder(final @NotNull String pathText) {
        if (pathText.isBlank()) return false;

        try {
            return Files.isDirectory(Path.of(pathText));
        } catch (final InvalidPathException ex) {
            return false;
        }
    }

    private void setupField() {
        ((JBTextField) pathField.getTextField()).getEmptyText()
                .setText(Bundle.message("settings.path.example"));

        pathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.singleDir()
                        .withTitle(Bundle.message("settings.path.root.title"))
                        .withDescription(Bundle.message("settings.path.root.description")),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
    }

    // Rule-SETTING-016
    private void setupOpenButton() {
        openFolderButton.setIcon(AllIcons.Actions.MenuOpen);
        openFolderButton.setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Actions.MenuOpen));
        openFolderButton.setEnabled(false);
        openFolderButton.addActionListener(_ -> RevealFileAction.openDirectory(Path.of(pathField.getText())));
    }

    private void setupValidationListener() {
        pathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                updateOpenButtonState();
            }
        });
    }

    // UC-SETTING-003, Rule-SETTING-015, Rule-SETTING-017, Rule-SETTING-016
    private void updateOpenButtonState() {
        final @NotNull String pathText = pathField.getText();
        asked.set(pathText);

        probe.execute(() -> {
            if (!pathText.equals(asked.get())) return;

            final boolean folder = isFolder(pathText);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (pathText.equals(asked.get())) openFolderButton.setEnabled(folder);
            }, ModalityState.any());
        });
    }

    // UC-SETTING-002, UC-SETTING-003
    public @NotNull JBPanel<?> getComponent() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout(5, 0));
        panel.add(pathField, BorderLayout.CENTER);
        panel.add(openFolderButton, BorderLayout.EAST);
        return panel;
    }

    public @NotNull String getPathText() {
        return pathField.getText();
    }

    public void setPathText(final @NotNull String text) {
        pathField.setText(text);
    }
}
