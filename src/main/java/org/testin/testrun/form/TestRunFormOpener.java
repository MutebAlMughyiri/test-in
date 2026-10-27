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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestProjectIndexer;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.services.Services;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.Bundle;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TestRunFormOpener {
    private final @NotNull Project project;

    private final @NotNull TestProjectIndexer indexer;

    private final @NotNull TestCases testCases;

    private final @NotNull Nodes nodes;

    public TestRunFormOpener(final @NotNull Project project) {
        this.project = project;
        this.indexer = Services.getInstance(project, TestProjectIndexer.class);
        this.testCases = Services.getInstance(project, TestCases.class);
        this.nodes = Services.getInstance(project, Nodes.class);
    }

    public static @NotNull Set<UUID> checkedTestCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> testCaseUuids = new LinkedHashSet<>();

        selection.forEachChecked(checked -> {
            if (checked instanceof TestCase testCase) testCaseUuids.add(testCase.getUuid());
        });

        return testCaseUuids;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-076
    public static @NotNull Set<UUID> offeredTestCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> testCaseUuids = new LinkedHashSet<>();

        selection.forEachLeaf(leaf -> {
            if (leaf instanceof TestCase testCase) testCaseUuids.add(testCase.getUuid());
        });

        return testCaseUuids;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-134
    private static @NotNull CheckedTreeNode deletedFolder(final @NotNull List<TestCase> deleted) {
        final @NotNull CheckedTreeNode folder = new CheckedTreeNode(Bundle.message("test.run.deleted.test.cases"));
        deleted.forEach(testCase -> folder.add(new CheckedTreeNode(testCase)));
        return folder;
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, UC-TREE-PANEL-022
    public void open(final @NotNull AbstractNode testCasesRoot, final @NotNull String name, final @NotNull Set<UUID> checked, final @NotNull List<TestCase> deleted, final @NotNull Map<TestRunConfiguration, String> configuration, final @NotNull TestRunFormPurpose action) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesRoot.getPath(), testCasesRoot);

            final @NotNull CheckedTreeNode root = convertToCheckedNodes(fullModelNode);
            if (!deleted.isEmpty()) root.add(deletedFolder(deleted));
            if (!checked.isEmpty()) checkOnly(root, checked);

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull TestRunConfigurationForm form = new TestRunConfigurationForm(project, name, configuration);

                new TestRunConfigurationDialog(project, form, new TestRunFormFilter(project, testCasesRoot.getPath(), root), action).show();
            });
        });
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-093, Rule-TREE-PANEL-076
    private boolean checkOnly(final @NotNull CheckedTreeNode treeNode, final @NotNull Set<UUID> wanted) {
        if (treeNode.getUserObject() instanceof TestCase testCase) {
            final boolean covered = wanted.contains(testCase.getUuid());
            treeNode.setChecked(covered);

            return covered;
        }

        boolean allCovered = treeNode.getChildCount() > 0;
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            allCovered &= checkOnly((CheckedTreeNode) treeNode.getChildAt(i), wanted);
        }

        treeNode.setChecked(allCovered);

        return allCovered;
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-030
    private @NotNull DefaultMutableTreeNode buildDirectoryTree(final @NotNull Path folder, final @NotNull AbstractNode thisNode) {
        indexer.awaitIndexing();

        final @NotNull DefaultMutableTreeNode node = new DefaultMutableTreeNode(thisNode);

        if (thisNode.holdsTestCases()) {
            for (final TestCase testCase : testCases.getTestCasesForTestSet(folder)) {
                node.add(new DefaultMutableTreeNode(testCase));
            }
            return node;
        }

        for (final AbstractNode child : nodes.getChildren(folder)) {
            final @NotNull DefaultMutableTreeNode childNode = buildDirectoryTree(child.getPath(), child);

            if (childNode.getChildCount() > 0) node.add(childNode);
        }

        return node;
    }

    // Rule-TREE-PANEL-008, Rule-TREE-PANEL-030, Rule-TREE-PANEL-076, Rule-TREE-PANEL-138, Rule-TREE-PANEL-139
    private @NotNull CheckedTreeNode convertToCheckedNodes(final @NotNull DefaultMutableTreeNode node) {
        final @NotNull Object userObject = node.getUserObject();
        final @NotNull CheckedTreeNode newNode = new CheckedTreeNode(userObject);
        if (userObject instanceof AbstractNode folder) newNode.setEnabled(!folder.isRetiredOrUnderOne());
        if (userObject instanceof TestCase testCase) {
            newNode.setEnabled(testCase.canJoinATestRun());
            newNode.setChecked(testCase.canJoinATestRun());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }
}
