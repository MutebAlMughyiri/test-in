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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.ui.Caption;
import org.testin.util.FailureText;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TextInput implements DialogComponent, TextValue {
    static final @NotNull String ANYTHING = ".*";

    private final @NotNull FrameworkTextField input;
    private final @NotNull JTextField textField;
    private final @NotNull JComponent panel;

    private @NotNull Optional<Runnable> chooser = Optional.empty();

    TextInput(final @NotNull Icon icon, final @NotNull String caption, final @NotNull String placeHolderText, final @NotNull String initialValue, final @NotNull String accepts) {
        input = new FrameworkTextField(icon, placeHolderText, initialValue);
        textField = input.component();
        // Rule-INTERNAL-122
        if (caption.isEmpty()) textField.getAccessibleContext().setAccessibleName(placeHolderText);
        // Rule-INTERNAL-087
        panel = caption.isEmpty() ? textField : Caption.above(caption, textField);

        if (!ANYTHING.equals(accepts)) accept(Pattern.compile(accepts));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-110
    private void accept(final @NotNull Pattern pattern) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(final @NotNull FilterBypass bypass, final int offset, final String text, final AttributeSet attributes) {
                if (refuses(bypass, offset, 0, text)) return;

                try {
                    super.insertString(bypass, offset, text, attributes);
                } catch (final BadLocationException ex) {
                    Logger.warn("Could not insert at " + offset + ": " + FailureText.of(ex));
                }
            }

            @Override
            public void replace(final @NotNull FilterBypass bypass, final int offset, final int length, final String text, final AttributeSet attributes) {
                if (refuses(bypass, offset, length, text)) return;

                try {
                    super.replace(bypass, offset, length, text, attributes);
                } catch (final BadLocationException ex) {
                    Logger.warn("Could not replace " + length + " at " + offset + ": " + FailureText.of(ex));
                }
            }

            private boolean refuses(final @NotNull FilterBypass bypass, final int offset, final int length, final @Nullable String text) {
                final @NotNull String current;
                try {
                    current = bypass.getDocument().getText(0, bypass.getDocument().getLength());
                } catch (final BadLocationException ex) {
                    Logger.warn("Could not read the field to check it: " + FailureText.of(ex));
                    return false;
                }

                final @NotNull String next = current.substring(0, offset) + Objects.requireNonNullElse(text, "")
                        + current.substring(offset + length);

                return !next.isEmpty() && !pattern.matcher(next).matches();
            }
        });
    }

    // Rule-INTERNAL-118, Rule-SETTING-021
    void browseWith(final @NotNull Project p, final @NotNull FileChooserDescriptor descriptor) {
        final @NotNull Runnable choose = () -> FileChooser.chooseFile(descriptor, p, textField,
                LocalFileSystem.getInstance().findFileByPath(getText().trim()), chosen -> textField.setText(chosen.getPresentableUrl()));

        chooser = Optional.of(choose);
        input.setBrowse(choose);
    }

    // Rule-INTERNAL-118
    public void browseNow() {
        chooser.ifPresent(Runnable::run);
    }

    @Override
    public @NotNull String getText() {
        return input.getText();
    }

    @Override
    public void showEmptyWarning() {
        input.showEmptyWarning();
    }

    public void onTextChanged(final @NotNull Runnable changed) {
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent event) {
                changed.run();
            }
        });
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return textField;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
