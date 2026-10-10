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

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ChoiceInput implements DialogComponent {
    private static final @NotNull String PICK = "testin.choice.pick";
    private final @NotNull JBPanel<?> panel;
    private final @NotNull ComboBox<String> combo;

    ChoiceInput(final @NotNull String caption, final @NotNull List<String> options, final @NotNull String selected) {
        combo = new ComboBox<>(options.toArray(String[]::new));
        combo.setEditable(true);
        DialogStyle.asChoice(combo);
        combo.setSelectedItem(selected);

        // Rule-INTERNAL-087
        panel = Caption.above(caption, combo);
        // Rule-INTERNAL-122
        combo.getEditor().getEditorComponent().getAccessibleContext().setAccessibleName(caption);

        enterPicksOnlyFromTheOpenList();

        combo.addPropertyChangeListener("editor", _ -> enterPicksOnlyFromTheOpenList());
    }

    // Rule-INTERNAL-055, Rule-INTERNAL-085
    private void enterPicksOnlyFromTheOpenList() {
        if (!(combo.getEditor().getEditorComponent() instanceof JComponent field)) return;

        final @NotNull KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

        if (PICK.equals(field.getInputMap().get(enter))) return;

        final @NotNull Optional<Action> pick = Optional.ofNullable(field.getInputMap().get(enter)).map(key -> field.getActionMap().get(key));
        if (pick.isEmpty()) return;

        field.getInputMap().put(enter, PICK);
        field.getActionMap().put(PICK, new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return combo.isPopupVisible();
            }

            @Override
            public void actionPerformed(final @NotNull ActionEvent e) {
                pick.orElseThrow().actionPerformed(e);
            }
        });
    }

    public @NotNull String getValue() {
        return Objects.toString(combo.getEditor().getItem(), "").trim();
    }

    public boolean isNew() {
        final @NotNull String value = getValue();

        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equals(combo.getItemAt(i))) return false;
        }
        return !value.isEmpty();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return combo;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    @Override
    public boolean canFillSpace() {
        return false;
    }
}
