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
import org.testin.indexer.TestCases;
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

record UpdateTestCaseWork(@NotNull Project p, @NotNull TestinEditor editor, @NotNull TestCases testCases, @NotNull Notifier notifier) {
    UpdateTestCaseWork(final @NotNull Project p, final @NotNull TestinEditor editor) {
        this(p, editor, Services.getInstance(p, TestCases.class), Services.getInstance(p, Notifier.class));
    }

    void overSelection(final @NotNull Consumer<TestCaseUpdateMenuDialog> open) {
        final @NotNull List<TestCaseDto> selectedItems = editor.getSelectedTestCases();
        if (selectedItems.isEmpty()) return;

        final @NotNull Path path = editor.getParent().getPath();

        Logger.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

        final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(selectedItems);
        final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, path, ids);

        open.accept(new TestCaseUpdateMenuDialog(p, selectedItems, (updatedItems, field) -> ApplicationManager.getApplication().executeOnPooledThread(() ->
                save(path, ids, before, updatedItems, field))));
    }

    private void save(final @NotNull Path path, final @NotNull List<UUID> ids, final @NotNull TestCaseSnapshot before, final @NotNull List<TestCaseDto> updatedItems, final @NotNull UpdateTestCaseFields field) {
        int counted = 0;
        for (final TestCaseDto tc : updatedItems)
            if (testCases.putTestCase(path, tc)) counted++;
        final int written = counted;

        if (written == 0) return;

        TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), updatedItems), before, TestCaseSnapshot.of(p, path, ids));

        ApplicationManager.getApplication().invokeLater(() -> {
            // Rule-EDITOR-PANEL-008
            notifier.softShowCounted(p, field.getDone(), written);

            editor.onToolBarFilterSelectionChanged();

            editor.refreshOrdered();
            TestCaseUpdateMenuDialog.applyAftermath(p, updatedItems, field.getGt());
        });
    }
}
