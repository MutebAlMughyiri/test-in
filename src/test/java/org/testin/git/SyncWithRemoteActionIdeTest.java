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

package org.testin.git;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.actions.TestinDataKeys;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.util.Bundle;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public class SyncWithRemoteActionIdeTest extends AbstractTempRootIdeTest {

    private TestProjectNode testProject;

    private TestSetNode login;

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
    }

    private @NotNull AnActionEvent pressedWith(final @NotNull DataContext data) {
        return TestActionEvent.createTestEvent(new SyncWithRemoteAction(), data);
    }

    private boolean offeredOn(final @NotNull AbstractNode selected) {
        final @NotNull SyncWithRemoteAction sync = new SyncWithRemoteAction();
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(sync, SimpleDataContext.builder().add(CommonDataKeys.PROJECT, getProject()).add(TestinDataKeys.SELECTED_NODES, List.of(selected)).build());
        ActionUtil.updateAction(sync, e);
        return e.getPresentation().isEnabled();
    }

    // UC-SHARE-016
    public void testSyncIsOfferedOnATestProjectAndGrayOnATestSet() {
        assertTrue("Sync is gray on a test project", offeredOn(testProject));
        assertFalse("Sync is offered on a test set", offeredOn(login));
    }

    // UC-SHARE-016
    public void testSyncWithNoTreeToReadSaysItCouldNotFindTheTestProject() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull SyncWithRemoteAction sync = new SyncWithRemoteAction();

        ActionUtil.performAction(sync, pressedWith(SimpleDataContext.getProjectContext(getProject())));

        assertEquals("Sync with no tree did not say it could not find the test project", List.of(Bundle.message("git.sync.error.title") + "\n" + Bundle.message("git.sync.no.test.project")), said.shown());
    }

    // UC-SHARE-016
    public void testSyncOnATestSetSyncsTheTestProjectAboveIt() {
        final @NotNull DefaultMutableTreeNode top = new DefaultMutableTreeNode(testProject);
        top.add(new DefaultMutableTreeNode(login));
        final @NotNull SimpleTree tree = new SimpleTree(new DefaultTreeModel(top));
        tree.setSelectionRow(1);
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        ActionUtil.performAction(new SyncWithRemoteAction(), pressedWith(SimpleDataContext.builder().add(CommonDataKeys.PROJECT, getProject()).add(TestinDataKeys.TREE, tree).build()));

        Await.until("Sync on a test set did not sync the test project above it", () -> said.shown().contains(Bundle.message("git.sync.nothing.title") + "\n" + Bundle.message("git.sync.nothing.message", "NAFATH")));
    }
}
