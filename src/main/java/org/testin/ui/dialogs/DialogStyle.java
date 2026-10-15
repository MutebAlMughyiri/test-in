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

package org.testin.ui.dialogs;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextIcon;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.NamedColorUtil;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.Spacing;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.util.Icons;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogStyle {
    public static final @NotNull Icon NO_ICON = EmptyIcon.ICON_0;

    private static final int MIN_CHARACTERS = 30;

    // Rule-INTERNAL-105
    private static final @NotNull Color ACCENT = JBUI.CurrentTheme.Button.defaultButtonColorStart();

    // Rule-INTERNAL-099
    private static final @NotNull Color CONTENT = JBColor.namedColor("Recap.cardBackground", JBColor.namedColor("WelcomeScreen.Details.background", UIUtil.getPanelBackground()));

    public static <T extends JComponent> @NotNull T styleContent(final @NotNull T component) {
        component.setOpaque(true);
        component.setBackground(ground());
        return component;
    }

    // Rule-INTERNAL-099
    static @NotNull Color card() {
        return panelIsTheContent() ? UIUtil.getPanelBackground() : CONTENT;
    }

    // Rule-INTERNAL-099
    private static @NotNull Color ground() {
        return panelIsTheContent() ? CONTENT : UIUtil.getPanelBackground();
    }

    private static boolean panelIsTheContent() {
        final boolean panelIsLighter = ColorUtil.getLuminance(UIUtil.getPanelBackground()) >= ColorUtil.getLuminance(CONTENT);

        return panelIsLighter == JBColor.isBright();
    }

    // Rule-INTERNAL-099
    public static @NotNull JBPanel<?> section(final @NotNull JComponent top, final @NotNull JComponent content) {
        final @NotNull JBPanel<?> panel = asSection(new JBPanel<>(new BorderLayout(0, JBUI.scale(Spacing.M))));
        panel.add(top, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    // Rule-INTERNAL-099
    public static <T extends JComponent> @NotNull T asSection(final @NotNull T component) {
        component.setOpaque(false);
        component.setBorder(new SectionFill());
        return component;
    }

    // Rule-INTERNAL-105
    public static void asPrimary(final @NotNull JButton button) {
        button.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, true);
    }

    // Rule-INTERNAL-105
    public static @NotNull Icon asAction(final @NotNull Icon icon) {
        return IconUtil.colorize(icon, ACCENT);
    }

    // Rule-INTERNAL-133
    public static @NotNull Icon chevron(final boolean expanded) {
        return asAction(expanded ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight);
    }

    // Rule-INTERNAL-095
    public static @NotNull JBLabel hint() {
        final @NotNull JBLabel hint = new JBLabel();
        hint.setFont(Fonts.hint());
        hint.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        return hint;
    }

    // Rule-INTERNAL-133
    public static void showFold(final @NotNull JComponent header, final @NotNull JBLabel title, final @NotNull JBLabel hint, final boolean expanded) {
        title.setIcon(chevron(expanded));
        hint.setText(expanded ? Bundle.message("panel.collapse") : Bundle.message("panel.expand"));
        Tooltip.set(header, expanded ? Bundle.message("panel.collapse.tooltip") : Bundle.message("panel.expand.tooltip"));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-096
    public static void framed(final @NotNull JComponent component) {
        component.setBorder(new FieldFrame());
    }

    // Rule-INTERNAL-095, Rule-INTERNAL-096
    public static void asField(final @NotNull JComponent component) {
        component.setFont(Fonts.field());
        component.setBorder(JBUI.Borders.empty(Spacing.L, Spacing.XL));
        hint(component);
    }

    // Rule-INTERNAL-095, Rule-INTERNAL-096
    public static void asChoice(final @NotNull JComponent component) {
        component.setFont(Fonts.choice());
        hint(component);
    }

    // Rule-INTERNAL-095
    public static void asRow(final @NotNull JComponent component) {
        component.setFont(Fonts.row());
        hint(component);
    }

    // Rule-INTERNAL-095
    public static void asOption(final @NotNull JComponent component) {
        component.setFont(Fonts.option());
    }

    private static void hint(final @NotNull JComponent component) {
        if (component instanceof ComponentWithEmptyText hinted) hinted.getEmptyText().setFont(Fonts.placeholder());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-073
    public static void setDecorations(final @NotNull ExtendableTextField textField, final @NotNull Icon icon, final @NotNull String note, final @NotNull Optional<Runnable> browse) {
        final @NotNull List<ExtendableTextComponent.Extension> extensions = new ArrayList<>();

        if (!icon.equals(NO_ICON)) extensions.add(leading(icon));
        if (!note.isEmpty()) extensions.add(trailing(note, textField));
        browse.ifPresent(open -> extensions.add(browsing(open)));

        textField.setExtensions(extensions);

        textField.revalidate();
        textField.repaint();
    }

    private static @NotNull ExtendableTextComponent.Extension leading(final @NotNull Icon icon) {
        // UC-INTERNAL-007, Rule-INTERNAL-077
        final @NotNull Icon quiet = Icons.gray(icon);

        return new ExtendableTextComponent.Extension() {
            @Override
            public @NotNull Icon getIcon(final boolean hovered) {
                return quiet;
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(Spacing.M);
            }
        };
    }

    // Rule-INTERNAL-077, Rule-INTERNAL-118
    private static @NotNull ExtendableTextComponent.Extension browsing(final @NotNull Runnable open) {
        return ExtendableTextComponent.Extension.create(Icons.gray(AllIcons.General.OpenDisk), AllIcons.General.OpenDiskHover, Bundle.message("dialog.browse"), open);
    }

    private static @NotNull ExtendableTextComponent.Extension trailing(final @NotNull String note, final @NotNull ExtendableTextField textField) {
        final @NotNull TextIcon drawn = new TextIcon(note, NamedColorUtil.getInactiveTextColor(), textField.getBackground(), JBUI.scale(2));
        drawn.setFont(Fonts.keycap());
        drawn.setWithBorders(false);

        return new ExtendableTextComponent.Extension() {
            @Override
            public @NotNull Icon getIcon(final boolean hovered) {
                return drawn;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(Spacing.M);
            }
        };
    }

    // UC-INTERNAL-007, Rule-INTERNAL-076, Rule-EDITOR-PANEL-034
    public static @NotNull ComponentPopupBuilder createPopupBuilder(final @NotNull JComponent content, final @NotNull JComponent focusComponent, final @NotNull String title, final boolean dismissOnClickOutside) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(content, focusComponent)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(dismissOnClickOutside)
                .setMovable(false)
                .setResizable(false)
                .setMinSize(new Dimension(content.getFontMetrics(Fonts.body()).charWidth('m') * MIN_CHARACTERS, 0));
    }
}
