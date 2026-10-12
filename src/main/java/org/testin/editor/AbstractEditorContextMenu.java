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

package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.editor.grid.GridKeys;
import org.testin.editor.grid.NotWhileEditing;
import org.testin.model.TestCaseDto;
import org.testin.model.node.Node;
import org.testin.ui.ActionsMenu;
import org.testin.undo.TestinUndoAction;
import org.testin.undo.UndoDirection;
import org.testin.undo.UndoScope;

import java.util.Arrays;

public abstract class AbstractEditorContextMenu extends DefaultActionGroup {
    protected AbstractEditorContextMenu() {
        super("", true);
    }

    private static void bindGroup(final @NotNull DefaultActionGroup group, final @NotNull JBTable table) {
        for (final AnAction action : group.getChildren(ActionManager.getInstance())) {
            if (action instanceof DefaultActionGroup nested) {
                bindGroup(nested, table);
                continue;
            }

            if (claimedByTheGrid(action)) continue;

            NotWhileEditing.bind(table, action);
        }
    }

    private static boolean claimedByTheGrid(final @NotNull AnAction action) {
        return Arrays.stream(action.getShortcutSet().getShortcuts())
                .filter(KeyboardShortcut.class::isInstance)
                .map(shortcut -> ((KeyboardShortcut) shortcut).getFirstKeyStroke())
                .anyMatch(GridKeys.keptFromMenus()::contains);
    }

    public abstract void registerShortcuts(final @NotNull JBList<TestCaseDto> list);

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-213
    protected @NotNull DefaultActionGroup actions(final @NotNull Project p, final @NotNull Node dir, final @NotNull JBList<TestCaseDto> list) {
        final @NotNull DefaultActionGroup actions = ActionsMenu.group();

        actions.add(Declared.forMenu("Testin.CopyTestCase"));
        actions.add(Declared.forMenu("Testin.CopyTestCaseNode"));
        actions.add(Declared.forMenu("Testin.CutTestCaseNode"));
        actions.add(Declared.forMenu("Testin.PasteTestCaseNode"));
        actions.add(Declared.forMenu("Testin.RemoveTestCase"));

        actions.addSeparator();

        actions.add(new TestinUndoAction(p, list, UndoScope.of(dir.getPath()), UndoDirection.UNDO));
        actions.add(new TestinUndoAction(p, list, UndoScope.of(dir.getPath()), UndoDirection.REDO));

        return actions;
    }

    // Rule-EDITOR-PANEL-010
    public void bindShortcutsTo(final @NotNull JBTable table) {
        bindGroup(this, table);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}