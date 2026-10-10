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
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.ActionSystem;
import org.testin.ui.Tooltip;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

public abstract class AbstractIconButton extends JButton {
    private @NotNull Icon restIcon;
    private @NotNull Icon zoomedIcon;
    private final @NotNull Optional<String> shortcutText;
    private boolean hovered;
    private boolean on;

    public AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon) {
        this(tooltip, icon, Optional.empty());
    }

    public AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull Shortcuts shortcut) {
        this(tooltip, icon, Optional.of(shortcut.getShortcutText()));
    }

    public AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull String shortcutText) {
        this(tooltip, icon, Optional.of(shortcutText));
    }

    private AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull Optional<String> shortcutText) {
        super(null, icon);
        this.shortcutText = shortcutText;
        describe(tooltip);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setOpaque(false);

        this.restIcon = icon;
        this.zoomedIcon = Icons.zoomStandardIcon(icon, this);

        setIcon(Icons.zoomStandardIcon(AllIcons.Actions.Refresh, this));
        final @NotNull Dimension size = getPreferredSize();
        setIcon(restIcon);

        setDisabledIcon(IconLoader.getDisabledIcon(restIcon));

        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                setHovered(false);
            }
        });
    }

    // Rule-INTERNAL-119
    public static @NotNull AbstractIconButton of(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull Runnable onClick) {
        final @NotNull AbstractIconButton button = new AbstractIconButton(tooltip, icon) {
        };
        button.addActionListener(_ -> onClick.run());
        return button;
    }

    // Rule-INTERNAL-137
    public final void show(final @NotNull String tooltip, final @NotNull Icon icon) {
        restIcon = icon;
        zoomedIcon = Icons.zoomStandardIcon(icon, this);
        setIcon(hovered ? zoomedIcon : restIcon);
        setDisabledIcon(IconLoader.getDisabledIcon(icon));
        describe(tooltip);
    }

    // UC-EDITOR-PANEL-020, UC-EDITOR-PANEL-049
    protected final void showMenuBelow(final @NotNull ActionGroup menu) {
        JBPopupFactory.getInstance()
                .createActionGroupPopup(null, menu, DataManager.getInstance().getDataContext(this), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
                .showUnderneathOf(this);
    }

    // Rule-INTERNAL-122
    protected final void describe(final @NotNull String text) {
        getAccessibleContext().setAccessibleName(text);
        shortcutText.ifPresentOrElse(key -> Tooltip.set(this, text, key), () -> Tooltip.set(this, text));
    }

    @Override
    protected void fireActionPerformed(final @NotNull ActionEvent event) {
        ActionSystem.perform(this, ActionPlaces.TOOLBAR, ActionUiKind.TOOLBAR, () -> swingClick(event));
    }

    private void swingClick(final @NotNull ActionEvent event) {
        super.fireActionPerformed(event);
    }

    private void setHovered(final boolean isHovered) {
        this.hovered = isHovered;
        setIcon(isHovered ? zoomedIcon : restIcon);
        repaint();
    }

    // Rule-EDITOR-PANEL-248
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled && hovered) setHovered(false);
    }

    public void setOn(final boolean isOn) {
        if (on == isOn) return;

        this.on = isOn;
        repaint();
    }

    @Override
    protected void paintComponent(final @NotNull Graphics g) {
        if ((on || hovered) && isEnabled()) {
            ActionButtonLook.SYSTEM_LOOK.paintBackground(g, this, on ? ActionButtonComponent.PUSHED : ActionButtonComponent.POPPED);
        }

        super.paintComponent(g);
    }
}
