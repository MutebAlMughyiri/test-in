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

package org.testin.testcase.create;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.framework.ShortcutMenuPopup;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class TestCaseUpdateMenuDialog {
    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> items;
    private final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull UpdateTestCaseFields> updatedItems;

    public TestCaseUpdateMenuDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> items, final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull UpdateTestCaseFields> updatedItems) {
        this.p = p;
        this.items = items;
        this.updatedItems = updatedItems;
    }

    public static void applyAftermath(final @NotNull Project p, final @NotNull List<TestCaseDto> updated, final @NotNull GenType<TestCaseDto> gt) {
        ViewToolWindowFactory.refreshIfShowing(p, updated);

        TestinLogger.trace("Generating automation code for " + updated.size() + ": " + gt);

        // UC-CODEGEN-003, Rule-CODEGEN-019
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (updated.size() == 1) gt.execute(p, updated.getFirst());
            else gt.executeAll(p, updated);
        });
    }

    // UC-EDITOR-PANEL-006
    public void show() {
        final @NotNull String title = items.size() == 1
                ? Bundle.message("update.dialog.title.one")
                : Bundle.message("update.dialog.title.many", String.valueOf(items.size()));

        new ShortcutMenuPopup<>(p, title, UpdateTestCaseFields.values(), this::open)
                .refusing(field -> field == UpdateTestCaseFields.ORDER && items.size() > 1
                        ? Optional.of(Bundle.message("update.order.one.at.a.time"))
                        : Optional.empty())
                .show();
    }

    // UC-EDITOR-PANEL-006, UC-EDITOR-PANEL-007
    public void open(final @NotNull UpdateTestCaseFields field) {
        final @NotNull GenType<TestCaseDto> gt = field.getGt();
        TestinLogger.trace("Update field -> " + field.getName() + " | changeType = " + gt);

        if (items.size() == 1) {
            new UpdateTestCaseDialog(p, items.getFirst(), field, edited -> {
                TestinLogger.trace("Single Edit Save -> changeType = " + gt);
                updatedItems.accept(List.of(edited), field);
            }).show();

            return;
        }

        field.getBulkAction().execute(p, items, list -> {
            TestinLogger.trace("Bulk Edit Save -> changeType = " + gt);
            updatedItems.accept(list, field);
        });
    }
}
