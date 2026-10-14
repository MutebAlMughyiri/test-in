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

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

final class TextInputField {
    private final @NotNull ExtendableTextField field;

    private final @NotNull String placeholder;

    private @NotNull Icon icon;

    private @NotNull String note = "";

    private @NotNull Optional<Runnable> browse = Optional.empty();

    private boolean emptyWarningShown;

    TextInputField(final @NotNull Icon icon, final @NotNull String placeholder, final @NotNull String initialValue) {
        this.placeholder = placeholder;
        this.field = new ExtendableTextField(initialValue);
        this.icon = icon;
        DialogStyle.setDecorations(field, icon, note, browse);

        DialogStyle.asField(field);
        DialogStyle.framed(field);

        if (!placeholder.isBlank()) {
            field.getEmptyText().setText(placeholder);
            TextComponentEmptyText.setupPlaceholderVisibility(field);

            field.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(final @NotNull DocumentEvent e) {
                    if (!emptyWarningShown) return;

                    emptyWarningShown = false;
                    showPlaceholder();
                }
            });
        }

        bindClipboard(field);
    }

    static void bindClipboard(final @NotNull JTextComponent component) {
        final int menuMask = Shortcuts.menuMask();

        bind(component, KeyEvent.VK_V, menuMask, DefaultEditorKit.pasteAction);
        bind(component, KeyEvent.VK_C, menuMask, DefaultEditorKit.copyAction);
        bind(component, KeyEvent.VK_X, menuMask, DefaultEditorKit.cutAction);
        bind(component, KeyEvent.VK_A, menuMask, DefaultEditorKit.selectAllAction);
    }

    private static void bind(final @NotNull JTextComponent component, final int keyCode, @MagicConstant(flagsFromClass = InputEvent.class) final int modifiers, final @NotNull String actionName) {
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
    }

    void setLeadingIcon(final @NotNull Icon icon) {
        this.icon = icon;
        DialogStyle.setDecorations(field, icon, note, browse);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-073
    void setNote(final @NotNull String note) {
        this.note = note;
        DialogStyle.setDecorations(field, icon, note, browse);
    }

    // Rule-INTERNAL-118
    void setBrowse(final @NotNull Runnable browse) {
        this.browse = Optional.of(browse);
        DialogStyle.setDecorations(field, icon, note, this.browse);
    }

    @NotNull JTextField component() {
        return field;
    }

    @NotNull String getText() {
        return field.getText();
    }

    void showEmptyWarning() {
        emptyWarningShown = true;

        EmptyWarning.show(field, placeholder);
    }

    private void showPlaceholder() {
        if (placeholder.isBlank()) return;

        field.getEmptyText().clear();
        field.getEmptyText().appendText(placeholder, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        field.repaint();
    }
}
