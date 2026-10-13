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

package org.testin.editor.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

public class FocusSearchAction extends DumbAwareAction {
    private final @NotNull SearchText searchText;

    public FocusSearchAction(final @NotNull SearchText searchText, final @NotNull JComponent scope) {
        super(Bundle.message("toolbar.focus.search"));
        this.searchText = searchText;
        registerCustomShortcutSet(Shortcuts.FocusSearch.getCustomShortcut(), scope);
    }

    // UC-EDITOR-PANEL-019
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        searchText.getTextEditor().requestFocusInWindow();
        searchText.getTextEditor().selectAll();
    }
}
