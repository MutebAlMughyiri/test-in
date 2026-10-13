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
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractJsonSplitBulkSectionDialog extends AbstractFrameworkDialog {
    private final @NotNull List<TestCaseDto> selectedTestCases;
    private final @NotNull Consumer<List<TestCaseDto>> updatedTestCases;
    private final @NotNull BulkJsonEditors editors;

    private final @NotNull List<String> originalEscaped = new ArrayList<>();

    protected AbstractJsonSplitBulkSectionDialog(final @NotNull Project project, final @NotNull List<TestCaseDto> selectedTestCases, final @NotNull Consumer<List<TestCaseDto>> updatedTestCases) {
        super(project);
        this.selectedTestCases = selectedTestCases;
        this.updatedTestCases = updatedTestCases;

        title = getPopupTitle();

        editors = new BulkJsonEditors(project);
        buildContent();

        components = List.of(DialogPart.of(editors));

        shortcuts = List.of(
                StatusBarShortcut.save(this::submit),
                StatusBarShortcut.build(Shortcuts.TabNext, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.TabPrevious, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1, true)),
                StatusBarShortcut.build(Shortcuts.ArrowDown, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1, false)),
                StatusBarShortcut.build(Shortcuts.ArrowUp, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1, false)),
                StatusBarShortcut.build(Shortcuts.CaretOnEveryValue, Bundle.message("bulk.shortcut.all.carets"), editors::caretOnEveryValue),
                StatusBarShortcut.hint("Ctrl+Click", Bundle.message("shortcut.multi.caret")),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;

        editors.bindKeysToEditor(shortcuts);
    }

    protected abstract @NotNull String getPopupTitle();

    protected abstract @NotNull String getJsonFieldName();

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-039
    protected abstract @NotNull TestSetEditorFields attribute();

    // Rule-EDITOR-PANEL-005
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto testCase) {
        return attribute().gridValue(testCase);
    }

    protected @NotNull Optional<TestCaseDto> withValue(final @NotNull TestCaseDto testCase, final @NotNull String value) {
        return attribute().getImportSetter().execute(project, testCase, value);
    }

    protected boolean acceptsBlank() {
        return true;
    }

    // Rule-EDITOR-PANEL-224
    protected @NotNull Set<Integer> clashing(final @NotNull List<TestCaseDto> testCases, final @NotNull List<EditedValue> newValues) {
        return Set.of();
    }

    protected boolean showsDescriptionContext() {
        return true;
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-041, Rule-EDITOR-PANEL-206
    protected @NotNull List<TestCaseDto> applyValues(final @NotNull List<TestCaseDto> testCases, final @NotNull List<EditedValue> newValues) {
        final @NotNull List<TestCaseDto> written = new ArrayList<>();
        final @NotNull Set<Integer> clashing = clashing(testCases, newValues);

        int refused = 0;

        for (int i = 0; i < testCases.size(); i++) {
            final @NotNull EditedValue edited = newValues.get(i);
            if (!edited.changed()) continue;

            if (edited.value().isEmpty() && !acceptsBlank()) continue;

            if (clashing.contains(i)) continue;

            final @NotNull Optional<TestCaseDto> took = withValue(testCases.get(i), edited.value());
            if (took.isEmpty()) {
                refused++;
                continue;
            }

            written.add(took.orElseThrow());
        }

        TestSetEditorFields.sayWhatWasRefused(project, refused);

        return written;
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

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-042
    @Override
    protected void submit() {
        final @NotNull List<EditedValue> newValues = new ArrayList<>();

        for (int i = 0; i < selectedTestCases.size(); i++) {
            final int index = i;
            newValues.add(editors.valueAt(index)
                    .filter(current -> !current.equals(originalEscaped.get(index)))
                    .map(current -> EditedValue.of(BulkValueText.unescape(current).trim()))
                    .orElse(EditedValue.UNCHANGED));
        }

        updatedTestCases.accept(applyValues(selectedTestCases, newValues));

        closeOk();
    }

    private void buildContent() {
        final @NotNull StringBuilder left = new StringBuilder("[\n");
        final @NotNull StringBuilder right = new StringBuilder("[\n");
        final @NotNull List<int[]> editableRanges = new ArrayList<>();

        for (int i = 0; i < selectedTestCases.size(); i++) {
            final @NotNull TestCaseDto testCase = selectedTestCases.get(i);
            final @NotNull String escapedValue = BulkValueText.escape(getOriginalValue(testCase));
            originalEscaped.add(escapedValue);

            final @NotNull StringBuilder prefixText = new StringBuilder("  {\n    \"id\": \"")
                    .append(BulkValueText.escape(testCase.getId().toString())).append("\",\n");
            if (showsDescriptionContext()) {
                prefixText.append("    \"description\": \"").append(BulkValueText.escape(testCase.getDescription())).append("\",\n");
            }
            prefixText.append("    \"").append(getJsonFieldName()).append("\": \"");

            final @NotNull String prefix = prefixText.toString();
            final @NotNull String suffix = "\"\n  }";
            final @NotNull String comma = (i == selectedTestCases.size() - 1) ? "\n" : ",\n";

            left.append(prefix).append(escapedValue).append(suffix).append(comma);

            right.append(prefix);
            final int start = right.length();
            right.append(escapedValue);
            editableRanges.add(new int[]{start, right.length()});
            right.append(suffix).append(comma);
        }

        left.append("]");
        right.append("]");

        editors.setContent(left.toString(), right.toString(), editableRanges);
        editors.setOriginalTextSource(originalEscaped::get);
    }
}
