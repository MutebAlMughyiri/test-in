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
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class JsonArraySplitBulkSectionDialog extends AbstractFrameworkDialog {
    private static final @NotNull ItemRecorder RECORDS_NOTHING = (_, _, _, _) -> {
    };
    private final @NotNull List<TestCaseDto> selectedItems;
    private final @NotNull Consumer<List<TestCaseDto>> updatedItems;
    private final @NotNull BulkJsonEditors editors;
    private final @NotNull List<List<String>> originalValues = new ArrayList<>();
    private final @NotNull List<List<String>> activeValues = new ArrayList<>();
    private final @NotNull List<int[]> spanOwners = new ArrayList<>();

    protected JsonArraySplitBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p);
        this.selectedItems = selectedItems;
        this.updatedItems = updatedItems;

        title = getPopupTitle();

        for (final List<String> values : extractOriginalValues(selectedItems)) {
            final @NotNull List<String> current = new ArrayList<>(Objects.requireNonNullElse(values, List.of()));
            if (current.isEmpty()) current.add("");

            originalValues.add(new ArrayList<>(current));
            activeValues.add(new ArrayList<>(current));
        }

        editors = new BulkJsonEditors(p);
        editors.setOriginalTextSource(this::originalEscapedAt);
        render();

        components = List.of(ComponentDialogBase.of(editors));

        shortcuts = List.of(
                StatusBarShortcut.save(this::submit),
                StatusBarShortcut.hint("Shift+Enter", StatusBarShortcut.SAVE),
                StatusBarShortcut.build(Shortcuts.AddArrayItem, Bundle.message("bulk.shortcut.add"), this::addItemAtCarets),
                StatusBarShortcut.build(Shortcuts.RemoveArrayItem, Bundle.message("bulk.shortcut.remove"), this::removeItemAtCarets),
                StatusBarShortcut.build(Shortcuts.TabNext, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.TabPrevious, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1, true)),
                StatusBarShortcut.build(Shortcuts.ArrowDown, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.ArrowUp, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1, true)),
                StatusBarShortcut.build(Shortcuts.CaretOnEveryValue, Bundle.message("bulk.shortcut.all.carets"), this::caretOnEveryValue),
                StatusBarShortcut.hint("Ctrl+Click", Bundle.message("shortcut.multi.caret")),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;

        editors.bindKeysToEditor(shortcuts);
    }

    protected abstract @NotNull List<TestCaseDto> applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues);

    protected abstract @NotNull String getPopupTitle();

    protected abstract @NotNull String getArrayFieldName();

    protected abstract @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items);

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

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-041
    @Override
    protected void submit() {
        readEditorIntoValues();

        final @NotNull List<TestCaseDto> edited = new ArrayList<>();
        final @NotNull List<List<String>> editedValues = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (activeValues.get(i).equals(originalValues.get(i))) continue;

            edited.add(selectedItems.get(i));
            editedValues.add(activeValues.get(i));
        }

        updatedItems.accept(applyValues(edited, editedValues));

        closeOk();
    }

    // UC-EDITOR-PANEL-007
    private void addItemAtCarets() {
        readEditorIntoValues();

        final @NotNull List<Integer> spans = editors.indicesUnderCarets();
        if (spans.isEmpty()) return;

        for (final int span : spans) {
            final int[] owner = spanOwners.get(span);
            activeValues.get(owner[0]).add(owner[1] + 1, "");
        }

        final int[] focus = spanOwners.get(spans.getLast());
        render();
        focusItem(focus[0], focus[1] + 1);
    }

    // UC-EDITOR-PANEL-007
    private void removeItemAtCarets() {
        readEditorIntoValues();

        final @NotNull List<Integer> spans = editors.indicesUnderCarets();
        if (spans.isEmpty()) return;

        final int focusTc = spanOwners.get(spans.getLast())[0];
        int focusItem = 0;

        for (final int span : spans) {
            final int[] owner = spanOwners.get(span);
            final @NotNull List<String> items = activeValues.get(owner[0]);

            if (items.size() > 1) {
                items.remove(owner[1]);
                focusItem = Math.min(owner[1], items.size() - 1);
            } else {
                items.set(0, "");
                focusItem = 0;
            }
        }

        render();
        focusItem(focusTc, focusItem);
    }

    private void caretOnEveryValue() {
        readEditorIntoValues();
        editors.caretOnEveryValue();
    }

    private void readEditorIntoValues() {
        for (int span = 0; span < spanOwners.size() && span < editors.valueCount(); span++) {
            final int[] owner = spanOwners.get(span);
            editors.valueAt(span).ifPresent(text ->
                    activeValues.get(owner[0]).set(owner[1], BulkValueText.unescape(text)));
        }
    }

    private void render() {
        final @NotNull StringBuilder left = new StringBuilder("[\n");
        final @NotNull StringBuilder right = new StringBuilder("[\n");
        final @NotNull List<int[]> editableRanges = new ArrayList<>();

        spanOwners.clear();

        for (int i = 0; i < selectedItems.size(); i++) {
            final @NotNull TestCaseDto tc = selectedItems.get(i);
            final @NotNull String prefix = "  {\n    \"id\": \"" + BulkValueText.escape(tc.getId().toString())
                    + "\",\n    \"description\": \"" + BulkValueText.escape(tc.getDescription())
                    + "\",\n    \"" + getArrayFieldName() + "\": [\n";
            left.append(prefix);
            right.append(prefix);

            appendItems(left, originalValues.get(i), RECORDS_NOTHING, i);
            appendItems(right, activeValues.get(i), (start, end, testCase, item) -> {
                editableRanges.add(new int[]{start, end});
                spanOwners.add(new int[]{testCase, item});
            }, i);

            final @NotNull String suffix = "    ]\n  }";
            final @NotNull String comma = (i < selectedItems.size() - 1) ? ",\n" : "\n";
            left.append(suffix).append(comma);
            right.append(suffix).append(comma);
        }

        left.append("]");
        right.append("]");

        editors.setContent(left.toString(), right.toString(), editableRanges);
    }

    private void appendItems(final @NotNull StringBuilder out, final @NotNull List<String> items, final @NotNull ItemRecorder recorder, final int testCaseIndex) {
        for (int j = 0; j < items.size(); j++) {
            out.append("      \"");

            final int start = out.length();
            out.append(BulkValueText.escape(items.get(j)));

            recorder.record(start, out.length(), testCaseIndex, j);

            out.append("\"").append(j < items.size() - 1 ? "," : "").append("\n");
        }
    }

    private void focusItem(final int testCaseIndex, final int itemIndex) {
        for (int span = 0; span < spanOwners.size(); span++) {
            final int[] owner = spanOwners.get(span);
            if (owner[0] == testCaseIndex && owner[1] == itemIndex) {
                editors.focusValue(span);
                return;
            }
        }
    }

    private @NotNull String originalEscapedAt(final int span) {
        if (span >= spanOwners.size()) return "";

        final int[] owner = spanOwners.get(span);
        final @NotNull List<String> original = originalValues.get(owner[0]);

        return owner[1] < original.size() ? BulkValueText.escape(original.get(owner[1])) : "";
    }

    @FunctionalInterface
    private interface ItemRecorder {
        void record(int start, int end, int testCaseIndex, int itemIndex);
    }
}
