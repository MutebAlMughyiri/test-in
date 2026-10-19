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

package org.testin.ui.framework;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;

import javax.swing.JComponent;
import java.awt.KeyboardFocusManager;

// Rule-VIEW-PANEL-036
public final class PlainTextArea implements DialogComponent {
    private final @NotNull JBTextArea area;

    private final @NotNull JComponent panel;

    PlainTextArea(final @NotNull String placeholder, final @NotNull String value, final int rows) {
        area = new JBTextArea(value);
        DialogStyle.asField(area);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setColumns(50);

        if (!placeholder.isBlank()) {
            area.getEmptyText().setText(placeholder);
        }
        // Rule-INTERNAL-122
        area.getAccessibleContext().setAccessibleName(placeholder);

        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        TextInputField.bindClipboard(area);

        final @NotNull JBScrollPane scroll = new JBScrollPane(area);
        DialogStyle.framed(scroll);
        scroll.setViewportBorder(JBUI.Borders.empty());

        // Rule-INTERNAL-087
        panel = Caption.above("", scroll);
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return area;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
