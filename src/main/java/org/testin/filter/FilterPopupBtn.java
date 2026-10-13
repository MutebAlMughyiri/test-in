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
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.testin.actions.GrayWithReason;
import org.testin.editor.EditorColors;
import org.testin.editor.toolbar.ToolbarItem;
import org.testin.model.Automated;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;
import org.testin.util.Icons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class FilterPopupBtn extends AbstractIconButton implements ToolbarItem {
    @Getter
    @NotNull
    private final Set<String> selectedGroup = new HashSet<>();

    @Getter
    @NotNull
    private final Set<Priority> selectedPriority = new HashSet<>();

    @Getter
    @NotNull
    private final Set<String> selectedModule = new HashSet<>();

    @Getter
    @NotNull
    private final Set<RunItemStatus> selectedStatus = new HashSet<>();

    @Getter
    private final @NotNull Set<TestCaseStatus> selectedTestCaseStatus = new HashSet<>();

    @Getter
    private final Set<Automated> selectedAutomation = new HashSet<>();

    @Getter
    private final @NotNull Set<Path> selectedTestSet = new HashSet<>();

    private final @NotNull FilterSource source;

    @NotNull
    private final DefaultActionGroup cachedActionGroup;

    public FilterPopupBtn(final @NotNull FilterSource source) {
        super(Bundle.message("filter.button"), AllIcons.General.Filter);
        this.source = source;

        this.cachedActionGroup = buildActionGroup();

        addActionListener(_ -> showMenuBelow(cachedActionGroup));
        updateToolBarFilterState();
    }

    private @NotNull List<Set<?>> filters() {
        return List.of(selectedPriority, selectedGroup, selectedModule, selectedTestCaseStatus, selectedStatus, selectedAutomation, selectedTestSet);
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-098
    public int activeFilterCount() {
        return filters().stream().mapToInt(Set::size).sum();
    }

    public boolean hasActiveFilters() {
        return activeFilterCount() > 0;
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-009
    public void updateToolBarFilterState() {
        final int activeFiltersCount = activeFilterCount();

        setOn(activeFiltersCount > 0);

        if (activeFiltersCount == 0) {
            setText(null);
            describe(Bundle.message("filter.button"));
            setForeground(JBColor.foreground());
        } else {
            setText("(" + activeFiltersCount + ")");
            describe(Bundle.message("filter.button.active", String.valueOf(activeFiltersCount)));
            setForeground(EditorColors.FILTER_ACTIVE);
        }
    }

    // UC-EDITOR-PANEL-021
    public void resetToolBarFilter() {
        clearFilters();
        source.onToolBarFilterResetButtonClicked();
    }

    // UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099, Rule-EDITOR-PANEL-117
    public void clearFilters() {
        filters().forEach(Set::clear);
        updateToolBarFilterState();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    private @NotNull DefaultActionGroup buildActionGroup() {
        final @NotNull Runnable onChanged = () -> {
            updateToolBarFilterState();
            source.onToolBarFilterSelectionChanged();
        };

        final @NotNull DefaultActionGroup filterResetBtn = new DefaultActionGroup();

        filterResetBtn.add(new DumbAwareAction(Bundle.message("filter.reset"), Bundle.message("filter.reset.description"), AllIcons.Actions.Cancel) {
            // UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099
            @Override
            public void update(final @NotNull AnActionEvent e) {
                GrayWithReason.unless(this, e, hasActiveFilters(), Bundle.message("filter.reset.nothing"));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                resetToolBarFilter();
            }
        });
        filterResetBtn.addSeparator();

        final @NotNull DefaultActionGroup filterPriorityMenu = new DefaultActionGroup(TestSetEditorAttributes.PRIORITY.getName(), true);
        Arrays.stream(Priority.values()).forEach(priority ->
                filterPriorityMenu.add(new ToggleFilterAction<>(priority.getLabel(), Icons.dot(priority.getColor()),
                        priority, selectedPriority, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterPriorityMenu);

        final @NotNull DefaultActionGroup filterAutomationMenu = new DefaultActionGroup(Bundle.message("filter.automation"), true);
        Automated.FILTERABLE.forEach(a -> filterAutomationMenu.add(new ToggleFilterAction<>(a.getLabel(), a.getIcon(),
                a, selectedAutomation, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterAutomationMenu);

        final @NotNull ActionGroup filterGroupMenu = new ActionGroup(TestSetEditorAttributes.GROUP.getName(), true) {
            // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
            @Override
            public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
                final @NotNull List<AnAction> actions = new ArrayList<>();
                actions.add(new ToggleFilterAction<>(Groups.NONE, null,
                        Groups.NONE, selectedGroup, FilterMembership.plain(), onChanged));

                source.getAvailableGroups().stream().sorted().forEach(group ->
                        actions.add(new ToggleFilterAction<>(group, null,
                                group, selectedGroup, FilterMembership.plain(), onChanged)));

                return actions.toArray(new AnAction[0]);
            }
        };
        filterResetBtn.add(filterGroupMenu);

        final @NotNull ActionGroup filterModuleMenu = new ActionGroup(TestSetEditorAttributes.MODULE.getName(), true) {
            // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
            @Override
            public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
                final @NotNull List<String> orderedModules = new ArrayList<>(source.getAvailableModules());
                Collections.sort(orderedModules);

                final @NotNull List<AnAction> actions = new ArrayList<>();
                for (final String module : orderedModules) {
                    actions.add(new ToggleFilterAction<>(module, null,
                            module, selectedModule, FilterMembership.plain(), onChanged));
                }
                return actions.toArray(new AnAction[0]);
            }
        };
        filterResetBtn.add(filterModuleMenu);

        final @NotNull DefaultActionGroup filterTestCaseStatusMenu = new DefaultActionGroup(Bundle.message("filter.test.case.status"), true);
        Arrays.stream(TestCaseStatus.values()).forEach(s ->
                filterTestCaseStatusMenu.add(new ToggleFilterAction<>(s.getLabel(), null,
                        s, selectedTestCaseStatus, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterTestCaseStatusMenu);

        filterResetBtn.add(submenu(Bundle.message("filter.run.item.status"), source::hasRunItemStatuses, Bundle.message("filter.run.only"), () -> Arrays.stream(RunItemStatus.values())
                .<AnAction>map(s -> new ToggleFilterAction<>(s.getLabel(), null, s, selectedStatus, FilterMembership.plain(), onChanged))
                .toList()));

        // Rule-EDITOR-PANEL-260, Rule-TREE-PANEL-129
        filterResetBtn.add(submenu(Bundle.message("filter.test.set"), source::holdsSeveralTestSets, Bundle.message("filter.test.set.one"), () -> source.getAvailableTestSets().entrySet().stream()
                .<AnAction>map(testSet -> new ToggleFilterAction<>(testSet.getValue(), null, testSet.getKey(), selectedTestSet, FilterMembership.plain(), onChanged))
                .toList()));

        return filterResetBtn;
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    private static @NotNull AnAction submenu(final @NotNull String name, final @NotNull BooleanSupplier works, final @NotNull String whyNot, final @NotNull Supplier<List<AnAction>> entries) {
        return new ActionGroup(name, true) {
            @Override
            public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
                return entries.get().toArray(AnAction[]::new);
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                GrayWithReason.unless(this, e, works.getAsBoolean(), whyNot);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    @TestOnly
    @NotNull DefaultActionGroup menu() {
        return cachedActionGroup;
    }
}
