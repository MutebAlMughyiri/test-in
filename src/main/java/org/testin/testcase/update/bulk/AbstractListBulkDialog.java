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
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractListBulkDialog extends AbstractBulkDialog {
    private static final @NotNull EntryRecorder RECORDS_NOTHING = (_, _, _, _) -> {
    };

    private final @NotNull List<List<String>> originalValues = new ArrayList<>();

    private final @NotNull List<List<String>> activeValues = new ArrayList<>();

    private final @NotNull List<int[]> spanOwners = new ArrayList<>();

    // Rule-EDITOR-PANEL-045
    protected AbstractListBulkDialog(final @NotNull Project project, final @NotNull TestSetEditorFields field, final @NotNull String title, final @NotNull List<TestCase> selectedTestCases, final @NotNull Consumer<List<TestCase>> updatedTestCases) {
        super(project, field, title, selectedTestCases, updatedTestCases);

        for (final List<String> values : extractOriginalValues(selectedTestCases)) {
            final @NotNull List<String> current = new ArrayList<>(values);
            if (current.isEmpty()) current.add("");

            originalValues.add(new ArrayList<>(current));
            activeValues.add(new ArrayList<>(current));
        }

        editors.setOriginalTextSource(this::originalEscapedAt);
        render();

        bindKeys(List.of(
                StatusBarShortcut.build(Shortcuts.AddItem, Bundle.message("shortcut.add"), this::addEntryAtCarets),
                StatusBarShortcut.build(Shortcuts.RemoveArrayItem, Bundle.message("bulk.shortcut.remove"), this::removeEntryAtCarets)), this::caretOnEveryValue);
    }

    protected abstract @NotNull List<TestCase> applyValues(final @NotNull List<TestCase> testCases, final @NotNull List<List<String>> newValues);

    protected abstract @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCase> testCases);

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-041
    @Override
    protected void submit() {
        readEditorIntoValues();

        final @NotNull List<TestCase> edited = new ArrayList<>();
        final @NotNull List<List<String>> editedValues = new ArrayList<>();

        for (int i = 0; i < selectedTestCases.size(); i++) {
            if (activeValues.get(i).equals(originalValues.get(i))) continue;

            edited.add(selectedTestCases.get(i));
            editedValues.add(activeValues.get(i));
        }

        updatedTestCases.accept(applyValues(edited, editedValues));

        closeOk();
    }

    // UC-EDITOR-PANEL-007
    private void addEntryAtCarets() {
        readEditorIntoValues();

        final @NotNull List<Integer> spans = editors.indicesUnderCarets();
        if (spans.isEmpty()) return;

        for (final int span : spans) {
            final int[] owner = spanOwners.get(span);
            activeValues.get(owner[0]).add(owner[1] + 1, "");
        }

        final int[] focus = spanOwners.get(spans.getLast());
        render();
        focusEntry(focus[0], focus[1] + 1);
    }

    // UC-EDITOR-PANEL-007
    private void removeEntryAtCarets() {
        readEditorIntoValues();

        final @NotNull List<Integer> spans = editors.indicesUnderCarets();
        if (spans.isEmpty()) return;

        final int focusedTestCase = spanOwners.get(spans.getLast())[0];
        int focusEntry = 0;

        for (final int span : spans) {
            final int[] owner = spanOwners.get(span);
            final @NotNull List<String> entries = activeValues.get(owner[0]);

            if (entries.size() > 1) {
                entries.remove(owner[1]);
                focusEntry = Math.min(owner[1], entries.size() - 1);
            } else {
                entries.set(0, "");
                focusEntry = 0;
            }
        }

        render();
        focusEntry(focusedTestCase, focusEntry);
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

        for (int i = 0; i < selectedTestCases.size(); i++) {
            final @NotNull TestCase selectedTestCase = selectedTestCases.get(i);
            final @NotNull String prefix = head(selectedTestCase) + "[\n";
            left.append(prefix);
            right.append(prefix);

            appendEntries(left, originalValues.get(i), RECORDS_NOTHING, i);
            appendEntries(right, activeValues.get(i), (start, end, testCase, entry) -> {
                editableRanges.add(new int[]{start, end});
                spanOwners.add(new int[]{testCase, entry});
            }, i);

            final @NotNull String suffix = "    ]\n  }";
            final @NotNull String comma = (i < selectedTestCases.size() - 1) ? ",\n" : "\n";
            left.append(suffix).append(comma);
            right.append(suffix).append(comma);
        }

        left.append("]");
        right.append("]");

        editors.setContent(left.toString(), right.toString(), editableRanges);
    }

    private void appendEntries(final @NotNull StringBuilder out, final @NotNull List<String> entries, final @NotNull EntryRecorder recorder, final int testCaseIndex) {
        for (int j = 0; j < entries.size(); j++) {
            out.append("      \"");

            final int start = out.length();
            out.append(BulkValueText.escape(entries.get(j)));

            recorder.record(start, out.length(), testCaseIndex, j);

            out.append("\"").append(j < entries.size() - 1 ? "," : "").append("\n");
        }
    }

    private void focusEntry(final int testCaseIndex, final int entryIndex) {
        for (int span = 0; span < spanOwners.size(); span++) {
            final int[] owner = spanOwners.get(span);
            if (owner[0] == testCaseIndex && owner[1] == entryIndex) {
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
    private interface EntryRecorder {
        void record(int start, int end, int testCaseIndex, int entryIndex);
    }
}
