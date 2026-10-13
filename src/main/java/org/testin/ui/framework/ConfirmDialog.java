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

package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;

public final class ConfirmDialog extends AbstractFrameworkDialog {
    private final @NotNull Runnable onConfirm;

    public ConfirmDialog(final @NotNull Project project, final @NotNull String dialogTitle, final @NotNull String message, final @NotNull String from, final @NotNull String to, final @NotNull String confirmName, final @NotNull Runnable onConfirm) {
        this(project, dialogTitle, message, from, to, confirmName, onConfirm, List.of());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-099, Rule-INTERNAL-105, Rule-INTERNAL-108, Rule-INTERNAL-140
    public ConfirmDialog(final @NotNull Project project, final @NotNull String dialogTitle, final @NotNull String message, final @NotNull String from, final @NotNull String to, final @NotNull String confirmName, final @NotNull Runnable onConfirm, final @NotNull List<Alternative> alternatives) {
        super(project);
        this.onConfirm = onConfirm;

        title = dialogTitle;

        final @NotNull TestinRoot root = Services.getInstance(project, TestinRoot.class);
        final @NotNull List<Alternative> closing = alternatives.stream().map(alternative -> new Alternative(alternative.key(), alternative.name(), () -> {
            closeCancel();
            alternative.action().run();
        })).toList();

        components = List.of(
                DialogPart.confirmCard(message, root.place(from), root.place(to)),
                DialogPart.button(confirmName, closing));

        final @NotNull List<StatusBarShortcut> keys = new ArrayList<>();
        keys.add(StatusBarShortcut.build(Shortcuts.Enter, confirmName, this::submit));

        for (final Alternative alternative : closing)
            if (alternative.hasKey())
                keys.add(StatusBarShortcut.build(alternative.key(), alternative.name(), alternative.action()));

        if (alternatives.stream().noneMatch(alternative -> alternative.key() == Shortcuts.Escape))
            keys.add(StatusBarShortcut.cancel(this::closeCancel));
        shortcuts = List.copyOf(keys);
    }

    @Override
    protected void submit() {
        onConfirm.run();
        closeOk();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    @Override
    protected boolean replacesItsKind() {
        return true;
    }
}
