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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.actions.TestinEscapeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.report.GenerateReportAction;
import org.testin.testrun.SetTestRunStatusAction;
import org.testin.ui.ActionsMenu;
import org.testin.undo.TestinUndoAction;
import org.testin.undo.UndoDirection;
import org.testin.undo.UndoScope;

import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    private final @NotNull Project project;

    public TreeContextMenu(final @NotNull Project project, final @NotNull SimpleTree tree) {
        super("", true);
        this.project = project;

        add(Declared.forMenu("Testin.Open"));
        add(Declared.forMenu("Testin.CreateNode"));

        addSeparator();

        add(actionsSubMenu(List.of(
                Declared.forMenu("Testin.UpdateStatus")), List.of(
                new TestinUndoAction(project, tree, UndoScope.TREE, UndoDirection.UNDO),
                new TestinUndoAction(project, tree, UndoScope.TREE, UndoDirection.REDO),
                Declared.forMenu("Testin.ReCreateTestRun"),
                Declared.forMenu("Testin.RemoveNode"),
                Declared.forMenu("Testin.Rename"),
                Declared.forMenu("Testin.OrderNode"),
                Declared.forMenu("Testin.CopyNode"),
                Declared.forMenu("Testin.CutNode"),
                Declared.forMenu("Testin.PasteNode"))));

        addSeparator();
        add(Declared.forMenu("Testin.RunTests"));

        addSeparator();

        add(Declared.forMenu("Testin.Export"));

        add(Declared.forMenu("Testin.Import"));

        addSeparator();
        add(Declared.forMenu("Testin.SyncWithRemote"));
        add(Declared.forMenu("Testin.ViewPendingCommits"));

        addSeparator();
        add(Declared.forMenu("Testin.EditTestRun"));
        add(new SetTestRunStatusAction(project, tree));
        addSeparator();

        add(new GenerateReportAction(project, tree));

        add(Declared.forMenu("Testin.ShowNodeDetails"));
    }

    private static @NotNull DefaultActionGroup actionsSubMenu(final @NotNull List<? extends AnAction> nodeStatusGroups, final @NotNull List<? extends AnAction> rest) {
        final @NotNull DefaultActionGroup group = ActionsMenu.group();
        nodeStatusGroups.forEach(group::add);
        rest.forEach(group::add);
        return group;
    }

    public void registerShortcuts(final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        new TestinEscapeAction(project, tree, transferHandler);
        new OpenContextMenuAction(tree, this);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}