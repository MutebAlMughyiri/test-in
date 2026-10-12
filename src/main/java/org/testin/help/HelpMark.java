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


package org.testin.help;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;
import org.testin.util.Icons;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class HelpMark implements CustomStatusBarWidget {
    private static final int MARK_SIZE = 20;
    private static final @NotNull Icon QUIET = IconUtil.resizeSquared(AllIcons.General.QuestionDialog, MARK_SIZE);
    private static final @NotNull Icon WAITING = IconUtil.colorize(QUIET, Icons.RED, true);
    private static final int HINT_CHARACTERS = 48;
    private static final double SCREEN_PART = 0.25;
    private static final int GAP = 8;

    private final @NotNull Project p;

    private final @NotNull JBLabel entry = new JBLabel(QUIET);

    HelpMark(final @NotNull Project p) {
        this.p = p;

        Tooltip.set(entry, Bundle.message("help.title"));
        entry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entry.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent click) {
                showPopup();
            }
        });
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    static void redraw(final @NotNull Project p) {
        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(WindowManager.getInstance().getStatusBar(p))
                .map(statusBar -> statusBar.getWidget(HelpMarkFactory.ID))
                .filter(HelpMark.class::isInstance)
                .map(HelpMark.class::cast)
                .ifPresent(HelpMark::redraw), p.getDisposed());
    }

    private static @NotNull JBPanel<?> page() {
        final @NotNull JBPanel<?> page = new JBPanel<>();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(JBUI.Borders.empty(GAP, GAP * 2));
        return page;
    }

    private static @NotNull JComponent wrapped(final @NotNull String text, final int width) {
        final @NotNull JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setBorder(JBUI.Borders.empty());
        area.setFont(UIUtil.getLabelFont());
        area.setForeground(UIUtil.getLabelForeground());
        area.setSize(width, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(width, area.getPreferredSize().height));
        area.setMaximumSize(area.getPreferredSize());
        return area;
    }

    private static @NotNull JComponent left(final @NotNull JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    @Override
    public @NotNull String ID() {
        return HelpMarkFactory.ID;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return entry;
    }

    // UC-INTERNAL-009, Rule-INTERNAL-126
    @Override
    public void install(final @NotNull StatusBar statusBar) {
        redraw();
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    private void redraw() {
        final @NotNull List<SetupHint> hints = Services.getInstance(p, Hints.class).waiting();
        entry.setIcon(hints.isEmpty() ? QUIET : WAITING);
        Tooltip.set(entry, hints.isEmpty() ? Bundle.message("help.title") : hints.getFirst().text());
    }

    // UC-INTERNAL-009, Rule-INTERNAL-128
    private void showPopup() {
        final @NotNull List<SetupHint> hints = Services.getInstance(p, Hints.class).waiting();
        final @NotNull List<Guide> guides = Services.getInstance(p, Guides.class).offered();
        if (hints.isEmpty() && guides.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("help.nothing"));
            return;
        }

        final @NotNull JBTabbedPane content = new JBTabbedPane();
        if (!hints.isEmpty()) content.addTab(Bundle.message("guide.hint.page"), hintsPage(hints, hintWidth()));
        if (!guides.isEmpty()) content.addTab(Bundle.message("guide.list.title"), guidesPage(guides));

        final @NotNull JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, content)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .setResizable(false)
                .createPopup();
        popup.show(new RelativePoint(entry, new Point(entry.getWidth() - content.getPreferredSize().width, -content.getPreferredSize().height - JBUI.scale(GAP))));
    }

    // UC-INTERNAL-009, Rule-INTERNAL-128, Rule-INTERNAL-129
    private @NotNull JComponent guidesPage(final @NotNull List<Guide> guides) {
        final @NotNull JBPanel<?> page = page();
        for (final Guide guide : guides) page.add(left(guideLink(guide)));
        return page;
    }

    // UC-INTERNAL-009, Rule-INTERNAL-128
    private @NotNull JComponent guideLink(final @NotNull Guide guide) {
        return PopupLink.of(guide.getTitle(), () -> guide.open(p));
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127, Rule-INTERNAL-128
    private @NotNull JComponent hintsPage(final @NotNull List<SetupHint> hints, final int hintWidth) {
        final @NotNull JBPanel<?> page = page();
        for (final SetupHint hint : hints) {
            if (page.getComponentCount() > 0) page.add(Box.createVerticalStrut(JBUI.scale(GAP * 2)));

            final @NotNull Optional<JComponent> row = hint.form().map(Supplier::get);
            page.add(left(wrapped(hint.text(), row.map(shown -> Math.max(shown.getPreferredSize().width, hintWidth)).orElse(hintWidth))));
            row.ifPresent(shown -> page.add(left(shown)));
            page.add(left(guideLink(hint.step().getGuide())));
        }
        return page;
    }

    // UC-INTERNAL-009
    private int hintWidth() {
        final int characters = entry.getFontMetrics(UIUtil.getLabelFont()).charWidth('m') * HINT_CHARACTERS;
        return Math.min(characters, (int) (ScreenUtil.getScreenRectangle(entry).width * SCREEN_PART));
    }
}
