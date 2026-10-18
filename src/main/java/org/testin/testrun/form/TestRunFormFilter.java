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

package org.testin.testrun.form;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AutomationState;
import org.testin.filter.FilterAndSortChoice;
import org.testin.filter.FilterPopupButton;
import org.testin.filter.FilterSource;
import org.testin.filter.TestCaseFilter;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// UC-TREE-PANEL-009, Rule-TREE-PANEL-129
public final class TestRunFormFilter implements FilterSource {
    private final @NotNull AutomationState automation;

    private final @NotNull List<TestCase> offered = new ArrayList<>();

    private final @NotNull Map<Path, String> testSets = new LinkedHashMap<>();

    private final @NotNull FilterPopupButton button;

    private final @NotNull SelectionTree selection;

    public TestRunFormFilter(final @NotNull Project project, final @NotNull Path testCasesRoot, final @NotNull CheckedTreeNode root) {
        this.automation = Services.getInstance(project, AutomationState.class);
        collect(root, testCasesRoot);

        this.button = new FilterPopupButton(this);
        this.selection = new SelectionTree(Bundle.message("test.run.form.test.cases.caption"), root, TestCaseSelectionRenderer.create(), Optional.of(button));

        automation.read(project, offered, () -> {
            if (!button.getSelectedAutomation().isEmpty()) onToolBarFilterSelectionChanged();
        });
    }

    public @NotNull SelectionTree getSelection() {
        return selection;
    }

    // Rule-EDITOR-PANEL-260
    @Override
    public boolean holdsSeveralTestSets() {
        return true;
    }

    // Rule-TREE-PANEL-129
    @Override
    public @NotNull Map<Path, String> getAvailableTestSets() {
        return testSets;
    }

    // Rule-EDITOR-PANEL-095
    @Override
    public @NotNull List<TestCase> getTestCasesToFilter() {
        return Collections.unmodifiableList(offered);
    }

    // Rule-TREE-PANEL-129, Rule-TREE-PANEL-130
    @Override
    public void onToolBarFilterSelectionChanged() {
        final @NotNull FilterAndSortChoice wanted = FilterAndSortChoice.of(button, "");
        final @NotNull Set<UUID> shown = automation.matching(TestCaseFilter.filter(offered, wanted), wanted.automation()).stream()
                .map(TestCase::getUuid)
                .collect(Collectors.toSet());

        selection.show(leaf -> leaf instanceof TestCase testCase && shown.contains(testCase.getUuid()));
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
        onToolBarFilterSelectionChanged();
    }

    // Rule-TREE-PANEL-129
    private void collect(final @NotNull CheckedTreeNode treeNode, final @NotNull Path testCasesRoot) {
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            final @NotNull CheckedTreeNode child = (CheckedTreeNode) treeNode.getChildAt(i);

            if (child.getUserObject() instanceof TestCase testCase && treeNode.getUserObject() instanceof AbstractNode testSet) {
                offered.add(testCase);
                testSets.putIfAbsent(testSet.getPath(), testSet.nameUnder(testCasesRoot));
            } else {
                collect(child, testCasesRoot);
            }
        }
    }
}
