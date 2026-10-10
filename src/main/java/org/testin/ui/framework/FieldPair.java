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

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.ui.dialogs.CollapsiblePanel;
import org.testin.util.Bundle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.GridLayout;
import java.util.List;

// Rule-INTERNAL-137
public final class FieldPair implements DialogComponent {
    private static final double BOXES_SHARE = 0.60;

    private final @NotNull Project p;
    private final @NotNull String rememberedAs;
    private final @NotNull MultiLineField first;
    private final @NotNull MultiLineField second;
    private final @NotNull List<JComponent> sections;
    private final @NotNull JBPanel<?> panel = new JBPanel<>();
    private final @NotNull AbstractIconButton layoutButton;

    private @NotNull Runnable refit = () -> {
    };

    @Getter
    private boolean sideBySide;

    // Rule-INTERNAL-133, Rule-INTERNAL-137
    FieldPair(final @NotNull Project p, final @NotNull String rememberedAs, final @NotNull JBLabel firstTitle, final @NotNull MultiLineField first, final @NotNull JBLabel secondTitle, final @NotNull MultiLineField second) {
        this.p = p;
        this.rememberedAs = rememberedAs;
        this.first = first;
        this.second = second;
        this.sections = List.of(section(firstTitle, first), section(secondTitle, second));
        this.sideBySide = PropertiesComponent.getInstance().getBoolean(rememberedAs, false);
        this.layoutButton = AbstractIconButton.of(Bundle.message("dialog.pair.side.by.side"), AllIcons.Actions.SplitVertically, this::switchLayout);

        panel.setOpaque(false);
        layOut();
    }

    // Rule-INTERNAL-137, Rule-INTERNAL-139
    private static @NotNull JComponent section(final @NotNull JBLabel title, final @NotNull MultiLineField box) {
        return CollapsiblePanel.build(title, box.getPanel(), true, List.of(box.formatButton()));
    }

    // Rule-INTERNAL-137, Rule-INTERNAL-138
    public @NotNull JComponent layoutButton() {
        return layoutButton;
    }

    // Rule-INTERNAL-137
    public void switchLayout() {
        sideBySide = !sideBySide;
        PropertiesComponent.getInstance().setValue(rememberedAs, sideBySide, false);
        layOut();
        refit.run();
    }

    // Rule-INTERNAL-137
    private void layOut() {
        panel.removeAll();
        if (sideBySide) {
            panel.setLayout(new GridLayout(1, sections.size(), JBUI.scale(Spacing.XL), 0));
            sections.forEach(panel::add);
            layoutButton.show(Bundle.message("dialog.pair.stacked"), AllIcons.Actions.SplitHorizontally);
        } else {
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(sections.getFirst());
            panel.add(Box.createVerticalStrut(JBUI.scale(Spacing.XL)));
            panel.add(sections.getLast());
            layoutButton.show(Bundle.message("dialog.pair.side.by.side"), AllIcons.Actions.SplitVertically);
        }

        first.capHeight(this::boxCap);
        second.capHeight(this::boxCap);

        panel.revalidate();
        panel.repaint();
    }

    // Rule-INTERNAL-136, Rule-INTERNAL-137
    private int boxCap() {
        final int frame = DialogSize.frameOn(p).height;
        return frame <= 0 ? Integer.MAX_VALUE : (int) (frame * BOXES_SHARE / (sideBySide ? 1 : sections.size()));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return first.getFocusComponent();
    }

    // Rule-INTERNAL-097
    @Override
    public void hostedBy(final @NotNull DialogHost host, final @NotNull Runnable submit) {
        first.hostedBy(host, submit);
        second.hostedBy(host, submit);
        refit = host::refit;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    // Rule-INTERNAL-137
    @Override
    public boolean fillsSpace() {
        return true;
    }

    @TestOnly
    @NotNull List<JComponent> sections() {
        return sections;
    }
}
