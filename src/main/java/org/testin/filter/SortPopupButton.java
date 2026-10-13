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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.actions.GrayWithReason;
import org.testin.editor.toolbar.ToolbarItem;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;

import java.util.HashSet;
import java.util.Set;

public class SortPopupButton extends AbstractIconButton implements ToolbarItem {
    private final @NotNull Set<SortField> selectedSort = new HashSet<>(Set.of(SortField.ORDER));

    private final @NotNull Set<SortDirection> selectedDirection = new HashSet<>(Set.of(SortDirection.ASCENDING));

    private final @NotNull DefaultActionGroup menu = new DefaultActionGroup();

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274, Rule-EDITOR-PANEL-276, Rule-EDITOR-PANEL-278
    public SortPopupButton(final @NotNull FilterSource source) {
        super(Bundle.message("sort.button"), AllIcons.ObjectBrowser.Sorted);

        final @NotNull Runnable onChanged = () -> {
            showState();
            source.onToolBarFilterSelectionChanged();
        };

        menu.add(new DumbAwareAction(Bundle.message("sort.reset"), Bundle.message("sort.reset.description"), AllIcons.Actions.Cancel) {
            // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-279
            @Override
            public void update(final @NotNull AnActionEvent e) {
                GrayWithReason.unless(this, e, !sortedBy().isEmpty(), Bundle.message("sort.reset.nothing"));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                reset();
                source.onToolBarFilterSelectionChanged();
            }
        });
        menu.add(Separator.getInstance());

        for (final SortField field : SortField.values()) {
            menu.add(new ToggleFilterAction<>(field.getLabel(), null, field, selectedSort, FilterMembership.single(), onChanged,
                    () -> !field.isRunOnly() || source.hasRunItemStatuses(), Bundle.message("filter.run.only")));
        }
        menu.add(Separator.getInstance());
        for (final SortDirection direction : SortDirection.values()) {
            menu.add(new ToggleFilterAction<>(direction.getLabel(), null, direction, selectedDirection, FilterMembership.single(), onChanged));
        }

        addActionListener(_ -> showMenuBelow(menu));
        showState();
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    public @NotNull SortField sortBy() {
        return selectedSort.iterator().next();
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    public @NotNull SortDirection direction() {
        return selectedDirection.iterator().next();
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-117
    public void reset() {
        FilterMembership.<SortField>single().apply(SortField.ORDER, selectedSort, true);
        FilterMembership.<SortDirection>single().apply(SortDirection.ASCENDING, selectedDirection, true);
        showState();
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274, Rule-EDITOR-PANEL-280
    public @NotNull String sortedBy() {
        final boolean sorted = sortBy() != SortField.ORDER || direction() != SortDirection.ASCENDING;
        return sorted ? Bundle.message("sort.button.active", sortBy().getLabel(), direction().getLabel()) : "";
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    private void showState() {
        final @NotNull String sortedBy = sortedBy();
        setOn(!sortedBy.isEmpty());
        describe(sortedBy.isEmpty() ? Bundle.message("sort.button") : sortedBy);
    }

    @TestOnly
    @NotNull DefaultActionGroup menu() {
        return menu;
    }
}
