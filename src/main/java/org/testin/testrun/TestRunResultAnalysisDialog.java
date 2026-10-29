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

package org.testin.testrun;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunSummary;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.MultiLineField;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class TestRunResultAnalysisDialog extends AbstractFrameworkDialog {
    private static final int FIRST_LINES = 3;

    private final @NotNull Map<TestRunResultAnalysis, MultiLineField> written = new EnumMap<>(TestRunResultAnalysis.class);

    private final @NotNull Consumer<@NotNull Map<TestRunResultAnalysis, String>> onSave;

    private final boolean readOnly;

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-190, Rule-EDITOR-PANEL-192
    public TestRunResultAnalysisDialog(final @NotNull Project project, final @NotNull TestRunSummary summary, final @NotNull Map<TestRunResultAnalysis, String> current, final @NotNull TestRunStatus testRunStatus, final @NotNull Consumer<@NotNull Map<TestRunResultAnalysis, String>> onSave) {
        super(project);
        this.onSave = onSave;
        this.readOnly = testRunStatus.isRecord();

        title = Bundle.message("dialog.analysis.title");

        final @NotNull List<DialogPart<?>> parts = new ArrayList<>();

        for (final TestRunResultAnalysis section : TestRunResultAnalysis.values()) {
            final @NotNull String placeholder = Bundle.message("dialog.analysis.placeholder", section.labelIn(testRunStatus).toLowerCase(Locale.ROOT));

            parts.add(DialogPart.message(section.heading(summary, testRunStatus)));
            parts.add(readOnly ? DialogPart.textArea().placeholder(placeholder).value(section.writtenIn(current)).rows(FIRST_LINES).build() : box(section, placeholder, section.writtenIn(current)));
        }

        // Rule-EDITOR-PANEL-189
        if (!readOnly) parts.add(DialogPart.button(StatusBarShortcut.SAVE));

        components = List.copyOf(parts);

        shortcuts = List.of(
                StatusBarShortcut.navigate(),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.TALL;
    }

    // Rule-EDITOR-PANEL-192, Rule-INTERNAL-097
    private @NotNull DialogPart<MultiLineField> box(final @NotNull TestRunResultAnalysis section, final @NotNull String placeholder, final @NotNull String value) {
        final @NotNull DialogPart<MultiLineField> box = DialogPart.paragraphField(project, placeholder, FIRST_LINES, value);
        written.put(section, box.getComponent());
        return box;
    }

    // UC-EDITOR-PANEL-045
    @Override
    protected void submit() {
        final @NotNull Map<TestRunResultAnalysis, String> analysis = new EnumMap<>(TestRunResultAnalysis.class);

        written.forEach((section, box) -> analysis.put(section, box.getText().trim()));

        onSave.accept(analysis);
        closeOk();
    }
}
