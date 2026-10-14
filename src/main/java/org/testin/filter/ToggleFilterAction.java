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

package org.testin.filter;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.GrayWithReason;

import javax.swing.Icon;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class ToggleFilterAction<T> extends DumbAwareToggleAction {
    private final @NotNull T value;

    private final @NotNull Set<T> selection;

    private final @NotNull FilterMembership<T> membership;

    private final @NotNull Runnable onChanged;

    private final @NotNull BooleanSupplier works;

    private final @NotNull String whyNot;

    ToggleFilterAction(final @NotNull String text, final @Nullable Icon icon, final @NotNull T value, final @NotNull Set<T> selection, final @NotNull FilterMembership<T> membership, final @NotNull Runnable onChanged) {
        this(text, icon, value, selection, membership, onChanged, () -> true, "");
    }

    ToggleFilterAction(final @NotNull String text, final @Nullable Icon icon, final @NotNull T value, final @NotNull Set<T> selection, final @NotNull FilterMembership<T> membership, final @NotNull Runnable onChanged, final @NotNull BooleanSupplier works, final @NotNull String whyNot) {
        super(text, null, icon);
        this.value = value;
        this.selection = selection;
        this.membership = membership;
        this.onChanged = onChanged;
        this.works = works;
        this.whyNot = whyNot;
    }

    // Rule-EDITOR-PANEL-094, Rule-EDITOR-PANEL-276
    @Override
    public void update(final @NotNull AnActionEvent e) {
        super.update(e);
        GrayWithReason.unless(this, e, works.getAsBoolean(), whyNot);
    }

    @Override
    public boolean isSelected(final @NotNull AnActionEvent e) {
        return selection.contains(value);
    }

    // UC-EDITOR-PANEL-020
    @Override
    public void setSelected(final @NotNull AnActionEvent e, final boolean state) {
        membership.apply(value, selection, state);
        onChanged.run();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
