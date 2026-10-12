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

package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CopiedTestCases;
import org.testin.clipboard.CutState;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JPanel;
import java.awt.datatransfer.StringSelection;

public class TestinEscapeAction extends AbstractProjectAction {
    private static final @NotNull String TITLE = Bundle.message("escape.title");

    private final @NotNull CutState cutState = Services.getInstance(p, CutState.class);

    private final @NotNull Runnable onEscape;

    public TestinEscapeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        super(p, TITLE, Bundle.message("escape.tree"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> clearTreeTransfer(transferHandler);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), tree);
    }

    public TestinEscapeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, TITLE, Bundle.message("escape.editor"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> stepBack(list::clearSelection);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), list);
    }

    // UC-VIEW-PANEL-015, Rule-VIEW-PANEL-088
    public TestinEscapeAction(final @NotNull Project p, final @NotNull JPanel tab) {
        super(p, TITLE, Bundle.message("escape.panel"), AllIcons.Actions.InlayGear);
        this.onEscape = this::giveTheKeyboardBack;
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), tab);
    }

    public TestinEscapeAction(final @NotNull Project p, final @NotNull JBTable table) {
        super(p, TITLE, Bundle.message("escape.grid"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> escapeInGrid(table);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), table);
    }

    // UC-TREE-PANEL-013, UC-EDITOR-PANEL-026, UC-VIEW-PANEL-015
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        onEscape.run();
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    private void clearTreeTransfer(final @NotNull TreeTransferHandler transferHandler) {
        transferHandler.clearClipboard();
        TestinLogger.info("Clipboard/Cut state cleared via ESC.");
    }

    // UC-EDITOR-PANEL-026, Rule-EDITOR-PANEL-115
    private void escapeInGrid(final @NotNull JBTable table) {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
            return;
        }

        stepBack(table::clearSelection);
    }

    // UC-EDITOR-PANEL-026, UC-VIEW-PANEL-015, Rule-EDITOR-PANEL-114, Rule-VIEW-PANEL-058
    private void stepBack(final @NotNull Runnable clearSelection) {
        if (dropWhatWaitsToBePasted()) return;
        if (ViewToolWindowFactory.panel(p).map(ViewPanel::hide).orElse(false)) return;

        clearSelection.run();
    }

    // UC-EDITOR-PANEL-026, Rule-EDITOR-PANEL-114, Rule-EDITOR-PANEL-262, Rule-VIEW-PANEL-058
    private boolean dropWhatWaitsToBePasted() {
        if (!cutState.isCutting() && CopiedTestCases.onTheClipboard(p).isEmpty()) return false;

        cutState.clear();
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        return true;
    }

    // UC-VIEW-PANEL-015, Rule-VIEW-PANEL-088
    private void giveTheKeyboardBack() {
        ToolWindowManager.getInstance(p).activateEditorComponent();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
