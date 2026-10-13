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

package org.testin.view.details;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public final class StacktraceDialog extends AbstractFrameworkDialog {
    private static final int VISIBLE_ROWS = 22;

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-036
    public StacktraceDialog(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull String actualResult, final @NotNull String stacktrace) {
        super(project);

        title = Bundle.message("dialog.stacktrace.title");

        size = DialogSize.TALL;

        components = List.of(
                DialogPart.details()
                        .row(CreateTestCaseFields.DESCRIPTION.getIcon(), testCase.getDescription())
                        .row(CreateTestCaseFields.EXPECTED_RESULT.getIcon(), testCase.getExpectedResult())
                        .row(TestRunEditorFields.ACTUAL_RESULT.getName(), actualResult)
                        .build(),
                DialogPart.textArea()
                        .value(stacktrace)
                        .rows(VISIBLE_ROWS)
                        .readOnly()
                        .build());

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));
    }

    @Override
    protected void submit() {
        closeOk();
    }
}
