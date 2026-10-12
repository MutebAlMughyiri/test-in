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
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
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

public final class TestRunForm {
    private final @NotNull Project p;
    private final @NotNull ProjectIndexer indexer;
    private final @NotNull TestCases testCases;
    private final @NotNull Nodes nodes;

    public TestRunForm(final @NotNull Project p) {
        this.p = p;
        this.indexer = Services.getInstance(p, ProjectIndexer.class);
        this.testCases = Services.getInstance(p, TestCases.class);
        this.nodes = Services.getInstance(p, Nodes.class);
    }

    public static @NotNull Set<UUID> checkedTestCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> ids = new LinkedHashSet<>();

        selection.forEachChecked(checked -> {
            if (checked instanceof TestCaseDto tc) ids.add(tc.getId());
        });

        return ids;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-076
    public static @NotNull Set<UUID> offeredTestCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> ids = new LinkedHashSet<>();

        selection.forEachLeaf(leaf -> {
            if (leaf instanceof TestCaseDto tc) ids.add(tc.getId());
        });

        return ids;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-134
    private static @NotNull CheckedTreeNode deletedFolder(final @NotNull List<TestCaseDto> deleted) {
        final @NotNull CheckedTreeNode folder = new CheckedTreeNode(Bundle.message("test.run.deleted.test.cases"));
        deleted.forEach(tc -> folder.add(new CheckedTreeNode(tc)));
        return folder;
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, UC-TREE-PANEL-022
    public void open(final @NotNull AbstractNode testCasesRoot, final @NotNull String name, final @NotNull Set<UUID> checked, final @NotNull List<TestCaseDto> deleted, final @NotNull Map<TestRunConfiguration, String> configuration, final @NotNull TestRunFormAction action) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesRoot.getPath(), testCasesRoot);

            final @NotNull CheckedTreeNode root = convertToCheckedNodes(fullModelNode);
            if (!deleted.isEmpty()) root.add(deletedFolder(deleted));
            if (!checked.isEmpty()) checkOnly(root, checked);

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull TestRunConfigurationForm form = new TestRunConfigurationForm(p, name, configuration);

                new TestRunConfigurationDialog(p, form, new TestRunFormFilter(p, testCasesRoot.getPath(), root), action).show();
            });
        });
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-093, Rule-TREE-PANEL-076
    private boolean checkOnly(final @NotNull CheckedTreeNode node, final @NotNull Set<UUID> wanted) {
        if (node.getUserObject() instanceof TestCaseDto tc) {
            final boolean covered = wanted.contains(tc.getId());
            node.setChecked(covered);

            return covered;
        }

        boolean allCovered = node.getChildCount() > 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            allCovered &= checkOnly((CheckedTreeNode) node.getChildAt(i), wanted);
        }

        node.setChecked(allCovered);

        return allCovered;
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-030
    private @NotNull DefaultMutableTreeNode buildDirectoryTree(final @NotNull Path folder, final @NotNull AbstractNode thisNode) {
        indexer.awaitIndexing();

        final @NotNull DefaultMutableTreeNode node = new DefaultMutableTreeNode(thisNode);

        if (thisNode.holdsTestCases()) {
            for (final TestCaseDto tc : testCases.getTestCasesForTestSet(folder)) {
                node.add(new DefaultMutableTreeNode(tc));
            }
            return node;
        }

        for (final AbstractNode child : nodes.getChildren(folder)) {
            if (child.isRetired()) continue;

            final @NotNull DefaultMutableTreeNode childNode = buildDirectoryTree(child.getPath(), child);

            if (childNode.getChildCount() > 0) node.add(childNode);
        }

        return node;
    }

    // Rule-TREE-PANEL-138, Rule-TREE-PANEL-139
    private @NotNull CheckedTreeNode convertToCheckedNodes(final @NotNull DefaultMutableTreeNode node) {
        final @NotNull Object userObj = node.getUserObject();
        final @NotNull CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        if (userObj instanceof TestCaseDto tc) newNode.setEnabled(tc.getStatus().isEnabled());
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }
}
