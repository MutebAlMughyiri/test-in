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

package org.testin.testcase;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.update.UpdateTestCaseMenu;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

record UpdateTestCaseWork(@NotNull Project project, @NotNull TestinEditor editor, @NotNull Notifier notifier) {
    UpdateTestCaseWork(final @NotNull Project project, final @NotNull TestinEditor editor) {
        this(project, editor, Services.getInstance(project, Notifier.class));
    }

    void overSelection(final @NotNull Consumer<UpdateTestCaseMenu> open) {
        final @NotNull List<TestCase> selectedTestCases = editor.getSelectedTestCases();
        if (selectedTestCases.isEmpty()) return;

        final @NotNull Path path = editor.getParent().getPath();

        TestinLogger.trace("update test cases: " + selectedTestCases.stream().map(TestCase::getDescription).collect(Collectors.joining(", ")));

        final @NotNull List<UUID> testCaseUuids = TestCaseSnapshot.uuidsOf(selectedTestCases);
        final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(project, path, testCaseUuids);

        open.accept(new UpdateTestCaseMenu(project, selectedTestCases, (updatedTestCases, field) -> ApplicationManager.getApplication().executeOnPooledThread(() ->
                save(path, testCaseUuids, before, updatedTestCases, field))));
    }

    // Rule-EDITOR-PANEL-006, Rule-EDITOR-PANEL-038, Rule-EDITOR-PANEL-039
    private void save(final @NotNull Path path, final @NotNull List<UUID> testCaseUuids, final @NotNull TestCaseSnapshot before, final @NotNull List<TestCase> updatedTestCases, final @NotNull UpdateTestCaseFields field) {
        final int written = UpdatedTestCases.write(project, path, testCaseUuids, before, updatedTestCases);
        if (written == 0) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            // Rule-EDITOR-PANEL-008
            notifier.softShowCounted(project, field.getDone(), written);

            editor.onToolBarFilterSelectionChanged();

            editor.refreshOrdered();
            UpdateTestCaseMenu.applyAftermath(project, updatedTestCases, field.getCodeChangeType());
        });
    }
}
