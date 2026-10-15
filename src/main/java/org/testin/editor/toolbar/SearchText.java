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

import com.intellij.openapi.Disposable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import java.awt.event.KeyEvent;

public class SearchText extends SearchTextField implements Disposable, ToolbarItem {
    private final @NotNull Timer searchDebounceTimer;

    private final @NotNull Runnable onFocusReleased;

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-090
    public SearchText(final @NotNull Runnable onToolBarSearchValueChanged, final @NotNull Runnable onFocusReleased) {
        super();
        this.onFocusReleased = onFocusReleased;

        setOpaque(false);
        getTextEditor().setOpaque(false);
        getTextEditor().setBackground(JBUI.CurrentTheme.EditorTabs.background());
        Tooltip.set(getTextEditor(), Bundle.message("search.tooltip",
                Shortcuts.FocusSearch.getShortcutText(), Shortcuts.Escape.getShortcutText()));

        searchDebounceTimer = new Timer(300, _ -> onToolBarSearchValueChanged.run());
        searchDebounceTimer.setRepeats(false);

        addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                searchDebounceTimer.restart();
            }
        });
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-093, Rule-EDITOR-PANEL-116
    @Override
    protected boolean preprocessEventForTextField(final KeyEvent e) {
        if (Shortcuts.Escape.matches(e) && e.getID() == KeyEvent.KEY_PRESSED) {
            e.consume();
            onFocusReleased.run();
            return true;
        }
        return super.preprocessEventForTextField(e);
    }

    public @NotNull String getSearchQuery() {
        return getText().trim();
    }

    @Override
    public void dispose() {
        if (searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }
    }
}
