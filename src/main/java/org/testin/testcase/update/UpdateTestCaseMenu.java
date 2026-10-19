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
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ShortcutMenuPopup;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@AllArgsConstructor
public class UpdateTestCaseMenu {
    private final @NotNull Project project;

    private final @NotNull List<TestCase> testCases;

    private final @NotNull BiConsumer<@NotNull List<TestCase>, @NotNull UpdateTestCaseFields> updatedTestCases;

    // Rule-VIEW-PANEL-049
    public static void applyAftermath(final @NotNull Project project, final @NotNull List<TestCase> updated, final @NotNull CodeChangeType<TestCase> codeChangeType) {
        ViewToolWindowFactory.refreshIfShowing(project, updated);

        TestinLogger.trace("Generating automation code for " + updated.size() + ": " + codeChangeType);

        // UC-CODEGEN-003, Rule-CODEGEN-019
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (updated.size() == 1) codeChangeType.execute(project, updated.getFirst());
            else codeChangeType.executeAll(project, updated);
        });
    }

    // UC-EDITOR-PANEL-006
    public void show() {
        final @NotNull String title = testCases.size() == 1
                ? Bundle.message("update.dialog.title.one")
                : Bundle.message("update.dialog.title.many", String.valueOf(testCases.size()));

        new ShortcutMenuPopup<>(project, title, UpdateTestCaseFields.values(), this::open)
                .refusing(this::refusalOf)
                .show();
    }

    // UC-EDITOR-PANEL-006, UC-EDITOR-PANEL-007, UC-EDITOR-PANEL-009
    public void open(final @NotNull UpdateTestCaseFields field) {
        final @NotNull Optional<String> refusal = refusalOf(field);
        if (refusal.isPresent()) {
            Services.getInstance(project, Notifier.class).softRefuse(project, refusal.orElseThrow());
            return;
        }

        final @NotNull CodeChangeType<TestCase> codeChangeType = field.getCodeChangeType();
        TestinLogger.trace("Update field -> " + field.getName() + " | changeType = " + codeChangeType);

        if (testCases.size() == 1) {
            new UpdateTestCaseDialog(project, testCases.getFirst(), field, edited -> {
                TestinLogger.trace("Single Edit Save -> changeType = " + codeChangeType);
                updatedTestCases.accept(edited, field);
            }).show();

            return;
        }

        field.getBulkAction().execute(project, testCases, list -> {
            TestinLogger.trace("Bulk Edit Save -> changeType = " + codeChangeType);
            updatedTestCases.accept(list, field);
        });
    }

    // UC-EDITOR-PANEL-006, UC-EDITOR-PANEL-009
    private @NotNull Optional<String> refusalOf(final @NotNull UpdateTestCaseFields field) {
        return field.isOneAtATime() && testCases.size() > 1 ? Optional.of(Bundle.message("update.order.one.at.a.time")) : Optional.empty();
    }
}
