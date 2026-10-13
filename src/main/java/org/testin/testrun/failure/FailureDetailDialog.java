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

package org.testin.testrun.failure;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FailureDetailDialog extends AbstractFrameworkDialog {
    private final @NotNull Consumer<FailureFields> onSave;
    private final @NotNull FailureFields fields;

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-143
    public FailureDetailDialog(final @NotNull Project project, final @NotNull Path testRunPath, final @NotNull RunItem runItem, final @NotNull Consumer<FailureFields> onSave) {
        super(project);
        this.onSave = onSave;

        final @NotNull TestCase testCase = runItem.shownTestCase();

        fields = new FailureFields(project, testRunPath, runItem);
        fields.onResized(this::refit);

        title = Bundle.message("dialog.failure.detail.title");

        // Rule-INTERNAL-101
        resizable = true;

        final @NotNull List<DialogPart<?>> all = new ArrayList<>();
        all.add(DialogPart.details()
                .row(CreateTestCaseFields.DESCRIPTION.getIcon(), testCase.getDescription())
                .row(CreateTestCaseFields.EXPECTED_RESULT.getIcon(), testCase.getExpectedResult())
                .build());
        all.addAll(fields.components());

        components = all;

        shortcuts = List.of(
                StatusBarShortcut.save(this::submit),
                StatusBarShortcut.cancel(this::closeCancel),
                StatusBarShortcut.corrections(),
                StatusBarShortcut.pasteScreenshot());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    @Override
    protected void submit() {
        onSave.accept(fields);
        closeOk();
    }
}
