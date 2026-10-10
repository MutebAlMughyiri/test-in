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

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;

// Rule-INTERNAL-096, Rule-INTERNAL-097
public final class MultiLineField implements DialogComponent {
    public static final int NO_LIMIT = Integer.MAX_VALUE;

    private static final int VISIBLE_LINES = 6;

    private final @NotNull Project p;
    private final @NotNull EditorTextField field;
    private final @NotNull String caption;
    private final int minimumLines;
    private final int maximumLines;

    private @NotNull IntSupplier heightCap = () -> Integer.MAX_VALUE;

    private final @NotNull List<Runnable> onGrow = new ArrayList<>();

    private @NotNull Optional<JComponent> panel = Optional.empty();

    private int packedHeight;

    // Rule-INTERNAL-058, Rule-INTERNAL-095, Rule-INTERNAL-096
    public MultiLineField(final @NotNull Project p, final @NotNull EditorTextField field, final @NotNull String caption, final @NotNull String placeholder) {
        this(p, field, caption, placeholder, 0, VISIBLE_LINES);
    }

    // Rule-INTERNAL-058, Rule-INTERNAL-095, Rule-INTERNAL-096, Rule-INTERNAL-136
    public MultiLineField(final @NotNull Project p, final @NotNull EditorTextField field, final @NotNull String caption, final @NotNull String placeholder, final int minimumLines, final int maximumLines) {
        this.p = p;
        this.field = field;
        this.caption = caption;
        this.minimumLines = minimumLines;
        this.maximumLines = maximumLines;

        DialogStyle.asField(field);
        // Rule-INTERNAL-087
        field.setBorder(JBUI.Borders.empty());
        field.setPlaceholder(placeholder);
        field.setShowPlaceholderWhenFocused(true);
        field.setOneLineMode(false);

        field.addDocumentListener(new BulkAwareDocumentListener.Simple() {
            @Override
            public void afterDocumentChange(final @NotNull Document document) {
                fitToLines();
                ApplicationManager.getApplication().invokeLater(MultiLineField.this::grew);
            }
        });

        field.addSettingsProvider(editor -> {
            editor.getContentComponent().setFocusTraversalKeysEnabled(true);

            editor.setBorder(new DarculaEditorTextFieldBorder(field, editor));
            // Rule-INTERNAL-102
            editor.setVerticalScrollbarVisible(true);
            // Rule-INTERNAL-136
            if (maximumLines == NO_LIMIT && editor.getScrollPane() instanceof JBScrollPane pane)
                pane.setOverlappingScrollBar(true);
            // Rule-INTERNAL-103
            editor.getSettings().setShowIntentionBulb(false);

            final @NotNull EditorColorsScheme themed = editor.createBoundColorSchemeDelegate(EditorColorsManager.getInstance().getSchemeForCurrentUITheme());
            final @NotNull Font font = Fonts.field();
            themed.setEditorFontName(font.getFontName());
            themed.setEditorFontSize(font.getSize());
            editor.setColorsScheme(themed);
        });

        if (minimumLines > 0) fitToLines();
    }

    // Rule-INTERNAL-136
    void capHeight(final @NotNull IntSupplier cap) {
        heightCap = cap;
        fitToLines();
    }

    // Rule-INTERNAL-139
    @NotNull JComponent formatButton() {
        final @NotNull FileType type = field.getFileType();
        return AbstractIconButton.of(Bundle.message("dialog.box.format", type.getDisplayName()), Objects.requireNonNullElse(type.getIcon(), AllIcons.Actions.ReformatCode), this::reformat);
    }

    // Rule-INTERNAL-139
    void reformat() {
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(p);
        WriteCommandAction.runWriteCommandAction(p, () -> {
            documents.commitDocument(field.getDocument());
            Optional.ofNullable(documents.getPsiFile(field.getDocument())).ifPresent(file -> CodeStyleManager.getInstance(p).reformat(file));
        });
    }

    // Rule-INTERNAL-060, Rule-EDITOR-PANEL-048, Rule-EDITOR-PANEL-246
    public void enableMultiLine(final @NotNull DialogHost host, final @NotNull Runnable onSave) {
        insertsNewLine(host);

        host.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), onSave);
    }

    // Rule-TREE-PANEL-122
    public void ignoresEnter(final @NotNull DialogHost host) {
        host.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), () -> {
        });
    }

    // Rule-EDITOR-PANEL-048, Rule-INTERNAL-097
    public void insertsNewLine(final @NotNull DialogHost host) {
        growsWith(host::refit);

        field.addSettingsProvider(editor -> host.registerShortcut(editor.getContentComponent(), Shortcuts.InsertNewLine.getCustomShortcut(), () -> insertNewLine(editor)));
    }

    // Rule-INTERNAL-097
    public void growsWith(final @NotNull Runnable refit) {
        onGrow.add(refit);
    }

    public @NotNull String getText() {
        return field.getText();
    }

    public void setText(final @NotNull String text) {
        field.setText(text);
    }

    public void setEnabled(final boolean enabled) {
        field.setEnabled(enabled);
    }

    // Rule-INTERNAL-087
    @Override
    public @NotNull JComponent getPanel() {
        if (panel.isEmpty()) panel = Optional.of(Caption.above(caption, field));

        return panel.orElseThrow();
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    // Rule-EDITOR-PANEL-246
    @Override
    public void hostedBy(final @NotNull DialogHost host, final @NotNull Runnable submit) {
        enableMultiLine(host, submit);
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    // Rule-EDITOR-PANEL-246
    private void insertNewLine(final @NotNull Editor editor) {
        final int caret = editor.getCaretModel().getOffset();
        WriteCommandAction.runWriteCommandAction(p, () -> {
            editor.getDocument().insertString(caret, "\n");
            editor.getCaretModel().moveToOffset(caret + 1);
        });

        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    // Rule-INTERNAL-102, Rule-INTERNAL-136
    private void fitToLines() {
        field.setPreferredSize(null);

        final @NotNull Dimension natural = field.getPreferredSize();
        final int line = field.getFontMetrics(Fonts.field()).getHeight();
        final int least = minimumLines == 0 ? 0 : line * minimumLines + field.getInsets().top + field.getInsets().bottom;
        final int most = Math.min(maximumLines == NO_LIMIT ? Integer.MAX_VALUE : line * maximumLines, heightCap.getAsInt());
        final int height = Math.clamp(natural.height, Math.min(least, most), most);
        if (height == natural.height) return;

        field.setPreferredSize(new Dimension(natural.width, height));
        field.revalidate();
    }

    // Rule-INTERNAL-097
    private void grew() {
        final int height = field.getPreferredSize().height;
        if (height == packedHeight) return;

        packedHeight = height;
        onGrow.forEach(Runnable::run);
    }
}
