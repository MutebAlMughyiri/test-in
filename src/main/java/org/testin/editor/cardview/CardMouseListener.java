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

package org.testin.editor.cardview;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.editor.WheelForwarding;
import org.testin.editor.card.AbstractCard;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Notifier;
import org.testin.quickaction.Offered;
import org.testin.services.Services;
import org.testin.ui.Tooltip;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Optional;

public class CardMouseListener extends MouseAdapter {
    private final @NotNull Project project;

    private final @NotNull CardList list;

    private final @NotNull CollectionListModel<TestCase> model;

    private final @NotNull AbstractEditorContextMenu contextMenu;

    private final @NotNull List<String> treePath;

    private final @NotNull TestinEditor editor;

    private final @NotNull Notifier notifier;

    private @NotNull String shownTip = "";

    public CardMouseListener(final @NotNull Project project, final @NotNull TestinEditor editor, final @NotNull CardList list, final @NotNull CollectionListModel<TestCase> model, final @NotNull AbstractNode node, final @NotNull AbstractEditorContextMenu contextMenu) {
        this.project = project;
        this.editor = editor;
        this.list = list;
        this.treePath = node.treePath();
        this.model = model;
        this.contextMenu = contextMenu;
        this.notifier = Services.getInstance(project, Notifier.class);
    }

    // UC-EDITOR-PANEL-024, UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-109, Rule-EDITOR-PANEL-110
    @Override
    public void mouseClicked(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final boolean isClickOnCard = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            if (isClickOnCard)
                Optional.ofNullable(model.getElementAt(index)).ifPresent(selected -> ViewToolWindowFactory.showPanel(project, List.of(selected), treePath, ViewPanel::focusDetailsTab));

            return;
        }

        if (!isClickOnCard) {
            list.getSelectionModel().clearSelection();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            if (isClickOnCard && !list.getSelectionModel().isSelectedIndex(index))
                list.getSelectionModel().setSelectionInterval(index, index);

            final @NotNull ActionManager actionManager = ActionManager.getInstance();
            final @NotNull String place = ActionPlaces.TOOLWINDOW_POPUP;
            actionManager.createActionPopupMenu(place, contextMenu).getComponent().show(e.getComponent(), e.getX(), e.getY());
        }
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-181
    @Override
    public void mousePressed(final MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;

        final int index = list.locationToIndex(e.getPoint());

        actionUnder(e, index).ifPresent(button -> {
            final @NotNull TestCase testCase = list.getModel().getElementAt(index);

            TestinLogger.trace(button.action().getTooltip() + ", test case: " + testCase.getDescription());

            if (!button.works()) {
                notifier.softRefuse(project, button.hintText());
                e.consume();
                return;
            }

            button.action().executeFor(editor, testCase);

            e.consume();
        });
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final int before = list.getHoveredIndex();
        final @NotNull Optional<Offered> currentAction = actionUnder(e, index);

        list.setCursor(Cursor.getPredefinedCursor(currentAction.isPresent() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        boolean needsRepaint = false;

        if (index != list.getHoveredIndex()) {
            list.setHoveredIndex(index);
            needsRepaint = true;
        }

        final @NotNull String actionName = currentAction.map(button -> button.action().name()).orElse("");

        if (!actionName.equals(list.getHoveredIconAction())) {
            list.setHoveredIconAction(actionName);
            needsRepaint = true;
        }

        final @NotNull String tip = currentAction.map(Offered::hintText).orElseGet(() -> tipUnder(e, index));
        if (!tip.equals(shownTip)) {
            shownTip = tip;
            Tooltip.set(list, tip);
        }

        if (!needsRepaint) return;

        repaintRow(before);
        repaintRow(index);
    }

    private void repaintRow(final int row) {
        if (row >= 0) Optional.ofNullable(list.getCellBounds(row, row)).ifPresent(list::repaint);
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        if (list.getHoveredIndex() != -1 || !list.getHoveredIconAction().isEmpty()) {
            final int before = list.getHoveredIndex();
            list.setHoveredIndex(-1);
            list.setHoveredIconAction("");
            shownTip = "";
            Tooltip.set(list, "");
            repaintRow(before);
        }
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        WheelForwarding.forwardWheelToScrollPane(e);
    }

    private @NotNull Optional<Offered> actionUnder(final @NotNull MouseEvent e, final int index) {
        if (index == -1) return Optional.empty();

        final @NotNull Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(e.getPoint())) return Optional.empty();

        final @NotNull Point inCell = new Point(e.getX() - bounds.x, e.getY() - bounds.y);
        return cardAt(index).flatMap(card -> card.actionAt(inCell));
    }

    // Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-269, Rule-EDITOR-PANEL-270
    private @NotNull String tipUnder(final @NotNull MouseEvent e, final int index) {
        if (index == -1) return "";

        final @NotNull Rectangle bounds = list.getCellBounds(index, index);
        final @NotNull Point inCell = new Point(e.getX() - bounds.x, e.getY() - bounds.y);
        return cardAt(index).map(card -> card.tooltipAt(inCell, bounds.getSize())).orElse("");
    }

    // Rule-EDITOR-PANEL-181, Rule-EDITOR-PANEL-267
    private @NotNull Optional<AbstractCard> cardAt(final int index) {
        final @NotNull Component drawn = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(index), index, false, false);
        return drawn instanceof AbstractCard card ? Optional.of(card) : Optional.empty();
    }
}
