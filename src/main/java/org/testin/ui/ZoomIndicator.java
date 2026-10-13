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

package org.testin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Bundle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class ZoomIndicator implements Disposable {
    private final @NotNull Project project;
    private final @NotNull Timer hideTimer = new Timer(5000, _ -> hide());
    private @NotNull Optional<JBPopup> currentPopup = Optional.empty();

    ZoomIndicator(final @NotNull Project project) {
        this.project = project;
        hideTimer.setRepeats(false);
    }

    // UC-SETTING-011
    public static void show(final @NotNull Project project, final @NotNull JComponent parent, final float currentSize) {
        Services.getInstance(project, ZoomIndicator.class).showIn(parent, currentSize);
    }

    private void hide() {
        currentPopup.filter(popup -> !popup.isDisposed()).ifPresent(JBPopup::cancel);
        currentPopup = Optional.empty();
    }

    private void showIn(final @NotNull JComponent parent, final float currentSize) {
        hide();

        if (!parent.isShowing()) return;

        final @NotNull JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(JBUI.Borders.empty(6, 12));
        DialogStyle.styleContent(panel);

        panel.add(new JBLabel(Bundle.message("zoom.font.size", String.valueOf((int) currentSize))));
        panel.add(Box.createHorizontalStrut(JBUI.scale(12)));

        final @NotNull JBLabel gearIcon = new JBLabel(AllIcons.General.GearPlain);
        gearIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gearIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                hide();
                if (!project.isDisposed()) {
                    ShowSettingsUtilImpl.showSettingsDialog(project, "preferences.editor", Bundle.message("zoom.change.font.size"));
                }
            }
        });
        panel.add(gearIcon);

        final @NotNull JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setCancelOnClickOutside(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .createPopup();
        currentPopup = Optional.of(popup);

        final @NotNull Dimension popupSize = panel.getPreferredSize();
        final @NotNull Rectangle visibleArea = parent.getVisibleRect();
        final int x = visibleArea.x + (visibleArea.width - popupSize.width) / 2;
        final int y = visibleArea.y + visibleArea.height - popupSize.height - JBUI.scale(25);

        popup.show(new RelativePoint(parent, new Point(x, y)));

        hideTimer.restart();
    }

    @Override
    public void dispose() {
        hideTimer.stop();
        hide();
    }
}
