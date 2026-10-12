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
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.editor.WheelForwarding;
import org.testin.editor.card.AbstractCard;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.card.CardTitle;
import org.testin.editor.card.Offered;
import org.testin.editor.testrun.ExecutingNow;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.Tooltip;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.awt.Component;
import java.awt.Point;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Optional;

public class CardMouseListener extends MouseAdapter {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull CollectionListModel<TestCaseDto> model;
    private final @NotNull AbstractEditorContextMenu cm;
    private final @NotNull List<String> path;
    private final @NotNull TestinEditor editor;
    private final @NotNull Notifier notifier;
    private @NotNull String shownTip = "";

    public CardMouseListener(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model, final @NotNull AbstractNode dir, final @NotNull AbstractEditorContextMenu cm) {
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.path = dir.getPath2();
        this.model = model;
        this.cm = cm;
        this.notifier = Services.getInstance(p, Notifier.class);
    }

    // UC-EDITOR-PANEL-024, UC-EDITOR-PANEL-025
    @Override
    public void mouseClicked(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final boolean isClickOnItem = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            if (isClickOnItem)
                Optional.ofNullable(model.getElementAt(index)).ifPresent(selected -> ViewToolWindowFactory.showPanel(p, List.of(selected), path, ViewPanel::focusDetailsTab));

            return;
        }

        if (!isClickOnItem) {
            list.getSelectionModel().clearSelection();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            if (isClickOnItem && !list.getSelectionModel().isSelectedIndex(index))
                list.getSelectionModel().setSelectionInterval(index, index);

            final @NotNull ActionManager actionManager = ActionManager.getInstance();
            final @NotNull String place = ActionPlaces.TOOLWINDOW_POPUP;
            actionManager.createActionPopupMenu(place, cm).getComponent().show(e.getComponent(), e.getX(), e.getY());
        }
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-181
    @Override
    public void mousePressed(final MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;

        final int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        final @NotNull Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(e.getPoint())) return;

        getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y).ifPresent(button -> {
            final @NotNull TestCaseDto tc = list.getModel().getElementAt(index);

            TestinLogger.trace(button.action().getTooltip() + ", tc: " + tc.getDescription());

            if (!button.works()) {
                notifier.softRefuse(p, button.hintText());
                e.consume();
                return;
            }

            button.action().executeFor(editor, tc);

            e.consume();
        });
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final int before = editor.getHoveredIndex();
        final @NotNull Optional<Offered> currentAction = actionUnder(e, index);

        list.setCursor(Cursor.getPredefinedCursor(currentAction.isPresent() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        boolean needsRepaint = false;

        if (index != editor.getHoveredIndex()) {
            editor.setHoveredIndex(index);
            needsRepaint = true;
        }

        final @NotNull String actionName = currentAction.map(button -> button.action().name()).orElse("");

        if (!actionName.equals(editor.getHoveredIconAction())) {
            editor.setHoveredIconAction(actionName);
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
        if (editor.getHoveredIndex() != -1 || !editor.getHoveredIconAction().isEmpty()) {
            final int before = editor.getHoveredIndex();
            editor.setHoveredIndex(-1);
            editor.setHoveredIconAction("");
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

        return getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);
    }

    private @NotNull Optional<Offered> getActionAtPoint(final int index, final int xInCell, final int yInCell) {
        if (index == -1) return Optional.empty();

        final @NotNull TestCaseDto tc = list.getModel().getElementAt(index);
        final @NotNull List<Offered> buttons = TestCaseQuickAction.onCard(p, editor.getParent(), tc);
        final boolean executing = ExecutingNow.is(p, tc.getId());
        final int titleWidth = CardTitle.titleWidth(list, editor.cardTitle(tc), CardTitle.slotCount(buttons, executing));

        return CardTitle.descriptionActionIcons(titleWidth, buttons, executing).at(xInCell, yInCell);
    }

    // Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-269, Rule-EDITOR-PANEL-270
    private @NotNull String tipUnder(final @NotNull MouseEvent e, final int index) {
        if (index == -1) return "";

        final @NotNull Rectangle bounds = list.getCellBounds(index, index);
        final @NotNull Point inCell = new Point(e.getX() - bounds.x, e.getY() - bounds.y);
        final @NotNull Component card = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(index), index, false, false);
        return card instanceof AbstractCard drawn ? drawn.tooltipAt(inCell, bounds.getSize()) : "";
    }
}
