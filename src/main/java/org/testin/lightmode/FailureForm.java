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

package org.testin.lightmode;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.failure.FailureFields;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogHost;
import org.testin.ui.framework.RowStripe;
import org.testin.util.Fonts;
import org.testin.util.Shortcuts;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class FailureForm extends JBPanel<FailureForm> implements DialogHost {
    private final @NotNull FailureFields fields;
    private final @NotNull Map<Component, Font> baseFonts = new HashMap<>();
    private final @NotNull Runnable resized;
    private final @NotNull Runnable onEnter;

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-202
    FailureForm(final @NotNull Project project, final @NotNull Path testRunPath, final @NotNull RunItem runItem, final float zoom, final @NotNull Runnable resized, final @NotNull Runnable onEnter) {
        this.fields = new FailureFields(project, testRunPath, runItem);
        this.resized = resized;
        this.onEnter = onEnter;

        fields.onResized(resized);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(JBUI.Borders.emptyTop(14));

        for (final DialogPart<?> component : fields.components()) {
            final @NotNull JComponent panel = component.getComponent().getPanel();
            panel.setAlignmentX(LEFT_ALIGNMENT);
            add(panel);

            // Rule-EDITOR-PANEL-219, Rule-INTERNAL-097
            component.getComponent().hostedBy(this, onEnter);
        }

        remember(this);
        setZoom(zoom);
    }

    // Rule-INTERNAL-097
    @Override
    public @NotNull JComponent root() {
        return this;
    }

    // UC-EDITOR-PANEL-046, Rule-INTERNAL-054, Rule-EDITOR-PANEL-246
    @Override
    public void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull Runnable action) {
        final @NotNull Runnable claimed = Shortcuts.Enter.is(shortcutSet) ? onEnter : action;

        DumbAwareAction.create((AnActionEvent _) -> claimed.run()).registerCustomShortcutSet(shortcutSet, component);
    }


    // Rule-EDITOR-PANEL-202
    @Override
    public void refit() {
        resized.run();
    }

    void setZoom(final float zoom) {
        baseFonts.forEach((component, base) -> {
            component.setFont(Fonts.zoomed(base, zoom));

            if (component instanceof ComponentWithEmptyText hinted)
                hinted.getEmptyText().setFont(Fonts.zoomed(Fonts.placeholder(), zoom));
        });
    }

    private void remember(final @NotNull Container parent) {
        for (final Component child : parent.getComponents()) {
            if (child.getFont() != null) baseFonts.put(child, child.getFont());

            if (child instanceof JTextComponent || child instanceof EditorTextField)
                child.setBackground(RowStripe.striped());

            if (child instanceof Container container) remember(container);
        }
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-256
    @NotNull FailureFields fields() {
        return fields;
    }

    void focusFirstField() {
        fields.actualResult().getFocusComponent().requestFocusInWindow();
    }
}
