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

package org.testin.testcase.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ShortcutMenuPopup;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class UpdateTestCaseMenu {
    private final @NotNull Project project;
    private final @NotNull List<TestCaseDto> testCases;
    private final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull UpdateTestCaseFields> updatedTestCases;

    public UpdateTestCaseMenu(final @NotNull Project project, final @NotNull List<TestCaseDto> testCases, final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull UpdateTestCaseFields> updatedTestCases) {
        this.project = project;
        this.testCases = testCases;
        this.updatedTestCases = updatedTestCases;
    }

    public static void applyAftermath(final @NotNull Project project, final @NotNull List<TestCaseDto> updated, final @NotNull GenType<TestCaseDto> gt) {
        ViewToolWindowFactory.refreshIfShowing(project, updated);

        TestinLogger.trace("Generating automation code for " + updated.size() + ": " + gt);

        // UC-CODEGEN-003, Rule-CODEGEN-019
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (updated.size() == 1) gt.execute(project, updated.getFirst());
            else gt.executeAll(project, updated);
        });
    }

    // UC-EDITOR-PANEL-006
    public void show() {
        final @NotNull String title = testCases.size() == 1
                ? Bundle.message("update.dialog.title.one")
                : Bundle.message("update.dialog.title.many", String.valueOf(testCases.size()));

        new ShortcutMenuPopup<>(project, title, UpdateTestCaseFields.values(), this::open)
                .refusing(field -> field == UpdateTestCaseFields.ORDER && testCases.size() > 1
                        ? Optional.of(Bundle.message("update.order.one.at.a.time"))
                        : Optional.empty())
                .show();
    }

    // UC-EDITOR-PANEL-006, UC-EDITOR-PANEL-007
    public void open(final @NotNull UpdateTestCaseFields field) {
        final @NotNull GenType<TestCaseDto> gt = field.getGt();
        TestinLogger.trace("Update field -> " + field.getName() + " | changeType = " + gt);

        if (testCases.size() == 1) {
            new UpdateTestCaseDialog(project, testCases.getFirst(), field, edited -> {
                TestinLogger.trace("Single Edit Save -> changeType = " + gt);
                updatedTestCases.accept(List.of(edited), field);
            }).show();

            return;
        }

        field.getBulkAction().execute(project, testCases, list -> {
            TestinLogger.trace("Bulk Edit Save -> changeType = " + gt);
            updatedTestCases.accept(list, field);
        });
    }
}
