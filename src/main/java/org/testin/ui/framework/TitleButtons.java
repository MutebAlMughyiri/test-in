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

import com.intellij.ui.ActiveComponent;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TitleButtons {
    // Rule-INTERNAL-101, Rule-INTERNAL-138
    static @NotNull ActiveComponent beside(final @NotNull List<JComponent> buttons, final @NotNull ActiveComponent maximize) {
        if (buttons.isEmpty()) return maximize;

        final @NotNull JBPanel<?> row = new JBPanel<>();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setBorder(JBUI.Borders.empty());
        buttons.forEach(row::add);
        row.add(maximize.getComponent());

        return new ActiveComponent() {
            @Override
            public void setActive(final boolean active) {
                maximize.setActive(active);
            }

            @Override
            public @NotNull JComponent getComponent() {
                return row;
            }
        };
    }
}
