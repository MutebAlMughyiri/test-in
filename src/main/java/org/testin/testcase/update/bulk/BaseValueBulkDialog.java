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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class BaseValueBulkDialog extends AbstractBulkDialog {
    private final boolean acceptsBlank;

    private final @NotNull List<String> originalEscaped = new ArrayList<>();

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-040, Rule-EDITOR-PANEL-298
    public BaseValueBulkDialog(final @NotNull Project project, final @NotNull TestSetEditorFields field, final @NotNull String title, final boolean acceptsBlank, final @NotNull List<TestCase> selectedTestCases, final @NotNull Consumer<List<TestCase>> updatedTestCases) {
        super(project, field, title, selectedTestCases, updatedTestCases);
        this.acceptsBlank = acceptsBlank;

        buildContent();
        bindKeys(List.of(), editors::caretOnEveryValue);
    }

    protected @NotNull Optional<TestCase> withValue(final @NotNull TestCase testCase, final @NotNull String value) {
        return field.getImportSetter().execute(project, testCase, value);
    }

    // Rule-EDITOR-PANEL-224
    protected @NotNull Set<Integer> clashing(final @NotNull List<TestCase> testCases, final @NotNull List<EditedValue> newValues) {
        return Set.of();
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-041, Rule-EDITOR-PANEL-206
    protected @NotNull List<TestCase> applyValues(final @NotNull List<TestCase> testCases, final @NotNull List<EditedValue> newValues) {
        final @NotNull List<TestCase> written = new ArrayList<>();
        final @NotNull Set<Integer> clashing = clashing(testCases, newValues);

        int refused = 0;

        for (int i = 0; i < testCases.size(); i++) {
            final @NotNull EditedValue edited = newValues.get(i);
            if (!edited.changed()) continue;

            if (edited.value().isEmpty() && !acceptsBlank) continue;

            if (clashing.contains(i)) continue;

            final @NotNull Optional<TestCase> took = withValue(testCases.get(i), edited.value());
            if (took.isEmpty()) {
                refused++;
                continue;
            }

            written.add(took.orElseThrow());
        }

        TestSetEditorFields.sayWhatWasRefused(project, refused);

        return written;
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

    // Rule-EDITOR-PANEL-005
    private void buildContent() {
        final @NotNull StringBuilder left = new StringBuilder("[\n");
        final @NotNull StringBuilder right = new StringBuilder("[\n");
        final @NotNull List<int[]> editableRanges = new ArrayList<>();

        for (int i = 0; i < selectedTestCases.size(); i++) {
            final @NotNull TestCase testCase = selectedTestCases.get(i);
            final @NotNull String escapedValue = BulkValueText.escape(field.gridValue(testCase));
            originalEscaped.add(escapedValue);

            final @NotNull String prefix = head(testCase) + "\"";
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
