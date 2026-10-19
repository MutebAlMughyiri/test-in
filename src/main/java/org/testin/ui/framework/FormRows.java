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

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;

import javax.swing.JComponent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

public final class FormRows extends JBPanel<FormRows> {
    private final @NotNull GridBagConstraints constraints = new GridBagConstraints();

    private int nextRow;

    public FormRows() {
        super(new GridBagLayout());
        setOpaque(false);

        constraints.insets = JBUI.insets(4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.weightx = 1.0;
    }

    // Rule-INTERNAL-087
    public @NotNull FormRows row(final @NotNull String label, final @NotNull JComponent field) {
        constraints.gridy = nextRow++;
        add(label.isEmpty() ? field : Caption.above(label, field), constraints);

        return this;
    }

    public @NotNull FormRows wideRow(final @NotNull JComponent component) {
        return row("", component);
    }

    // Rule-INTERNAL-087
    public @NotNull FormRows pair(final @NotNull JComponent left, final @NotNull JComponent right) {
        final @NotNull JBPanel<?> line = new JBPanel<>(new GridLayout(1, 2, JBUI.scale(Spacing.XL), 0));
        line.setOpaque(false);
        line.add(left);
        line.add(right);

        return wideRow(line);
    }
}
