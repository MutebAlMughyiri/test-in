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

package org.testin.editor.statusbar;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorStatusBarPaging {
    // UC-EDITOR-PANEL-022, UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-222
    public static void attach(final @NotNull TestinEditor editor) {
        editor.getStatusBar().getPageSizeField().setText(String.valueOf(editor.getPageSize()));

        for (final PageStep step : PageStep.values()) {
            editor.getStatusBar().button(step).addActionListener(
                    _ -> editor.stepPage(step.deltaFrom(editor.getCurrentPage(), editor.getTotalPageCount())));
        }

        editor.getStatusBar().getPageSizeField().addActionListener(_ -> {
            final @NotNull String typed = editor.getStatusBar().getPageSizeField().getText().trim();
            final int size = TestinEditor.pageSizeOf(typed);
            editor.getStatusBar().getPageSizeField().setText(String.valueOf(size));

            if (!typed.isEmpty() && typed.chars().allMatch(Character::isDigit)
                    && !typed.equals(String.valueOf(size))) {
                Services.getInstance(editor.getProject(), Notifier.class).softRefuse(editor.getProject(),
                        Bundle.message("statusbar.page.size.refused", String.valueOf(TestinEditor.MAX_PAGE_SIZE)));
            }

            final boolean changed = size != editor.getPageSize();
            editor.choosePageSize(size);

            if (changed) {
                editor.setCurrentPage(1);
                editor.refreshView();
            }

            editor.getPreferredFocusedComponent().requestFocusInWindow();
        });
    }
}