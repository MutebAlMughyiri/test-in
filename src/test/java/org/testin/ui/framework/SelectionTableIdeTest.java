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

import com.intellij.util.ui.NamedColorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;

import javax.swing.JTable;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class SelectionTableIdeTest extends AbstractIdeTest {

    private static final @NotNull String WHY = "A marker always goes with the commit";

    private @NotNull SelectionTable aReviewOfOneChangeAndOneMarker() {
        final @NotNull SelectionTable review = new SelectionTable(List.of("Change", "Where"), List.of(20, 20));
        review.addRow("Description", "Login");
        review.addFixedRow(WHY, "Marker", "Login");
        theTable(review).setSize(400, 200);
        return review;
    }

    private static @NotNull JTable theTable(final @NotNull SelectionTable review) {
        return (JTable) review.getFocusComponent();
    }

    private static @NotNull MouseEvent at(final @NotNull JTable table, final @NotNull Point point, final int type, final int modifiers, final int button) {
        return new MouseEvent(table, type, System.currentTimeMillis(), modifiers, point.x, point.y, 1, false, button);
    }

    private static @NotNull Point middleOf(final @NotNull JTable table, final int row) {
        final @NotNull Rectangle cell = table.getCellRect(row, 0, true);
        return new Point(cell.x + cell.width / 2, cell.y + cell.height / 2);
    }

    // UC-SHARE-010, Rule-SHARE-129
    public void testAMarkerRowIsTickedGraySaysWhyAndCannotBeUnticked() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        final @NotNull JTable table = theTable(review);

        assertEquals("the marker is not ticked", List.of(1), review.getSelectedRows());
        assertFalse("a row of the review can be typed into", table.isCellEditable(1, 0));
        assertEquals("the marker row is not gray", NamedColorUtil.getInactiveTextColor(), table.prepareRenderer(table.getCellRenderer(1, 0), 1, 0).getForeground());
        assertEquals("an unticked change row painted after the marker is gray like it", table.getForeground(), table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getForeground());
        assertEquals("the marker row does not say why when pointed at", WHY, table.getToolTipText(at(table, middleOf(table, 1), MouseEvent.MOUSE_MOVED, 0, MouseEvent.NOBUTTON)));

        table.addRowSelectionInterval(0, 0);
        assertEquals("a ticked change row is not drawn as the table draws a selected row", table.getSelectionForeground(), table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getForeground());
        table.removeRowSelectionInterval(0, 1);
        assertEquals("unticking both rows unticked the marker", List.of(1), review.getSelectedRows());
    }

    // UC-SHARE-010, Rule-SHARE-129
    public void testARightClickBelowTheRowsUnticksTheChangesButNotTheMarker() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        review.onRowAction("Revert", _ -> {
        });
        final @NotNull JTable table = theTable(review);
        review.selectAll();

        final @NotNull MouseEvent below = at(table, new Point(10, table.getRowHeight() * 5), MouseEvent.MOUSE_RELEASED, InputEvent.BUTTON3_DOWN_MASK, MouseEvent.BUTTON3);
        for (final MouseListener listener : table.getMouseListeners()) listener.mouseReleased(below);

        assertEquals("a right click below the rows did not untick the change, or unticked the marker", List.of(1), review.getSelectedRows());
    }
}
