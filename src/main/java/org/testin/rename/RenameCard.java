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

package org.testin.rename;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.Node;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.ui.dialogs.FormRows;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.DialogPlace;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.util.Icons;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.util.List;
import java.util.Optional;

// UC-TREE-PANEL-011, Rule-INTERNAL-099, Rule-TREE-PANEL-123
final class RenameCard implements DialogComponent {
    private static final int ICON_GAP = 8;

    private final @NotNull TextInput field;
    private final @NotNull JComponent panel;

    // UC-TREE-PANEL-011, Rule-INTERNAL-087, Rule-INTERNAL-099, Rule-INTERNAL-108, Rule-TREE-PANEL-123
    RenameCard(final @NotNull Node dir, final @NotNull List<String> place, final @NotNull TextInput field) {
        this.field = field;

        panel = DialogStyle.asSection(new FormRows()
                .pair(Caption.above(Bundle.message("caption.renaming"), renamed(dir)), Caption.above(Bundle.message("caption.in"), DialogPlace.of(place)))
                .wideRow(Caption.header(Caption.of(Bundle.message("caption.new.name"), Fonts.caption()), Optional.empty()))
                .wideRow(field.getPanel()));
    }

    // Rule-INTERNAL-077, Rule-INTERNAL-095, Rule-INTERNAL-096
    private static @NotNull JBLabel renamed(final @NotNull Node dir) {
        final @NotNull JBLabel named = new JBLabel(dir.getName(), Icons.gray(dir.iconShownInTree()), SwingConstants.LEADING);
        named.setFont(Fonts.value());
        named.setIconTextGap(JBUI.scale(ICON_GAP));

        return named;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field.getFocusComponent();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
