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

package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractBulkDialog extends AbstractFrameworkDialog {
    protected final @NotNull TestSetEditorFields field;

    protected final @NotNull List<TestCase> selectedTestCases;

    protected final @NotNull Consumer<List<TestCase>> updatedTestCases;

    final @NotNull BulkJsonEditors editors;

    protected AbstractBulkDialog(final @NotNull Project project, final @NotNull TestSetEditorFields field, final @NotNull String title, final @NotNull List<TestCase> selectedTestCases, final @NotNull Consumer<List<TestCase>> updatedTestCases) {
        super(project);
        this.field = field;
        this.title = title;
        this.selectedTestCases = selectedTestCases;
        this.updatedTestCases = updatedTestCases;
        this.editors = new BulkJsonEditors(project);

        components = List.of(DialogPart.of(editors));
        size = DialogSize.HALF;
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-298
    protected final void bindKeys(final @NotNull List<StatusBarShortcut> own, final @NotNull Runnable caretOnEveryValue) {
        final @NotNull List<StatusBarShortcut> all = new ArrayList<>();
        all.add(StatusBarShortcut.save(this::submit));
        all.addAll(own);
        all.add(StatusBarShortcut.build(Shortcuts.TabNext, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1)));
        all.add(StatusBarShortcut.build(Shortcuts.TabPrevious, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1)));
        all.add(StatusBarShortcut.build(Shortcuts.ArrowDown, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1)));
        all.add(StatusBarShortcut.build(Shortcuts.ArrowUp, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1)));
        all.add(StatusBarShortcut.build(Shortcuts.CaretOnEveryValue, Bundle.message("bulk.shortcut.all.carets"), caretOnEveryValue));
        all.add(StatusBarShortcut.hint("Ctrl+Click", Bundle.message("shortcut.multi.caret")));
        all.add(StatusBarShortcut.cancel(this::closeCancel));

        shortcuts = List.copyOf(all);
        editors.bindKeysToEditor(shortcuts);
    }

    // UC-EDITOR-PANEL-007
    public void open() {
        if (!show()) {
            editors.release();
            return;
        }

        getPopup().addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                editors.release();
            }
        });
        editors.focusFirstValue();
    }

    // Rule-EDITOR-PANEL-298
    final @NotNull String head(final @NotNull TestCase testCase) {
        final @NotNull StringBuilder head = new StringBuilder("  {\n    \"uuid\": \"").append(BulkValueText.escape(testCase.getUuid().toString())).append("\",\n");
        if (field != TestSetEditorFields.DESCRIPTION)
            head.append("    \"").append(TestSetEditorFields.DESCRIPTION.getStoredKey()).append("\": \"").append(BulkValueText.escape(testCase.getDescription())).append("\",\n");
        return head.append("    \"").append(field.getStoredKey()).append("\": ").toString();
    }
}
