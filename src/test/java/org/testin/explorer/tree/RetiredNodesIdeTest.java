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

package org.testin.explorer.tree;

import com.intellij.openapi.application.WriteAction;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.NodeStatus;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.services.Services;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RetiredNodesIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull List<String> drawn(final @NotNull AbstractNode node) {
        final @NotNull TreePanelCellRenderer renderer = new TreePanelCellRenderer(Set.of());
        renderer.getTreeCellRendererComponent(new JTree(), new DefaultMutableTreeNode(node), false, false, true, 0, false);

        final @NotNull List<String> fragments = new ArrayList<>();
        final @NotNull SimpleColoredComponent.ColoredIterator each = renderer.iterator();
        while (each.hasNext()) {
            final @NotNull String text = each.next();
            fragments.add(text + (each.getTextAttributes().equals(SimpleTextAttributes.GRAYED_ATTRIBUTES) ? " [grayed]" : ""));
        }
        return fragments;
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private void marked(final @NotNull AbstractNode node, final @NotNull NodeStatus status) {
        assertTrue("the status was not written", nodes().mark(node, status, "Sara"));
    }

    // Rule-PRODUCT-022
    public void testADeprecatedTestSetIsKeptAndDrawnAsDeprecated() {
        final @NotNull NodesOnDisk onDisk = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = onDisk.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = onDisk.testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestCaseDto testCase = onDisk.testCase(login);

        marked(login, TestSetStatus.DEPRECATED);

        assertTrue("deprecating the test set deleted its folder", Files.isDirectory(login.getPath()));
        assertTrue("deprecating the test set dropped it from the tree", nodes().find(login.getPath()).isPresent());
        assertTrue("deprecating the test set lost its test cases", Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getId()).isPresent());
        assertEquals("the deprecated test set is not drawn as deprecated", List.of(login.getName() + " [grayed]", " " + TestSetStatus.DEPRECATED.getLabel()), drawn(login));
    }

    // Rule-PRODUCT-023
    public void testActiveAndArchivedMeanTheSameForBothKindsOfPackage() {
        final @NotNull TestProjectNode testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
        final @NotNull TestSetPackageNode accounts = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Accounts");
        final @NotNull TestRunPackageNode release = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunPackageNode made = Services.getInstance(getProject(), NodeMapper.class).getTestRunPackageNode(testProject.getTestRunsFolder().getPath().resolve("Release"), testProject.getTestRunsFolder());
            nodes().addTestRunPackage(made);
            return made;
        });

        for (final PackageStatus status : PackageStatus.values()) {
            marked(accounts, status);
            marked(release, status);

            assertEquals(status + " sets a test set package and a test run package apart differently", accounts.isRetired(), release.isRetired());
            assertEquals(status + " leaves one kind of package out of Expand All and not the other", new TreePanelNode(getProject(), accounts).isIncludedInExpandAll(), new TreePanelNode(getProject(), release).isIncludedInExpandAll());
            assertEquals(status + " is drawn differently on the two kinds of package", drawn(accounts).stream().map(fragment -> fragment.replace(accounts.getName(), "")).toList(), drawn(release).stream().map(fragment -> fragment.replace(release.getName(), "")).toList());
        }
        assertTrue("an archived package is not set apart at all, so the two agreeing proves nothing", accounts.isRetired());
    }
}
