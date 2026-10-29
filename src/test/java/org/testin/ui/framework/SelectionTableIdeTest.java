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

import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.NamedColorUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JWindow;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
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

    private static @NotNull MouseEvent pointedAt(final @NotNull JTable table, final @NotNull Point point) {
        return new MouseEvent(table, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.x, point.y, 0, false, MouseEvent.NOBUTTON);
    }

    private static @NotNull MouseEvent rightClickedAt(final @NotNull JTable table, final @NotNull Point point) {
        return new MouseEvent(table, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, point.x, point.y, 1, false, MouseEvent.BUTTON3);
    }

    private static @NotNull Point onTheMarker(final @NotNull JTable table) {
        final @NotNull Rectangle cell = table.getCellRect(1, 0, true);
        return new Point(cell.x + cell.width / 2, cell.y + cell.height / 2);
    }

    // Rule-SHARE-129
    public void testAMarkerRowIsTickedGraySaysWhyAndCannotBeUnticked() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        final @NotNull JTable table = theTable(review);

        assertEquals("the marker is not ticked", List.of(1), review.getSelectedRows());
        assertFalse("a row of the review can be typed into", table.isCellEditable(1, 0));
        assertEquals("the marker row is not gray", NamedColorUtil.getInactiveTextColor(), table.prepareRenderer(table.getCellRenderer(1, 0), 1, 0).getForeground());
        assertEquals("an unticked change row painted after the marker is gray like it", table.getForeground(), table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getForeground());
        assertEquals("the marker row does not say why when pointed at", WHY, table.getToolTipText(pointedAt(table, onTheMarker(table))));

        table.addRowSelectionInterval(0, 0);
        assertEquals("a ticked change row is not drawn as the table draws a selected row", table.getSelectionForeground(), table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getForeground());
        table.removeRowSelectionInterval(0, 1);
        assertEquals("unticking both rows unticked the marker", List.of(1), review.getSelectedRows());
    }

    // Rule-SHARE-129
    public void testAMarkerAboveTheChangeTakenAwayStaysTickedWhereItIs() {
        final @NotNull SelectionTable review = new SelectionTable(List.of("Change", "Where"), List.of(20, 20));
        review.addFixedRow(WHY, "Marker", "Login");
        review.addRow("Description", "Login");
        final @NotNull JTable table = theTable(review);
        table.setSize(400, 200);

        review.removeRow(1);
        table.removeRowSelectionInterval(0, 0);

        assertEquals("the marker above the change taken away can be unticked", List.of(0), review.getSelectedRows());
    }

    // Rule-SHARE-129
    public void testAMarkerStaysTickedAndGrayWhenTheChangeAboveItIsTakenAway() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        final @NotNull JTable table = theTable(review);

        review.removeRow(0);

        assertEquals("the marker moved up is not ticked", List.of(0), review.getSelectedRows());
        assertEquals("the marker moved up is not gray", NamedColorUtil.getInactiveTextColor(), table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0).getForeground());
        table.removeRowSelectionInterval(0, 0);
        assertEquals("the marker moved up can be unticked", List.of(0), review.getSelectedRows());
    }

    private static void released(final @NotNull JTable table, final @NotNull MouseEvent event) {
        for (final MouseListener listener : table.getMouseListeners()) listener.mouseReleased(event);
    }

    private @NotNull JWindow aWindowOnScreen() {
        final @NotNull JWindow onScreen = new JWindow();
        onScreen.setBounds(-4000, -4000, 10, 10);
        onScreen.setVisible(true);
        Disposer.register(getTestRootDisposable(), onScreen::dispose);
        return onScreen;
    }

    // Rule-SHARE-118
    public void testARightClickOnAnUntickedRowTicksItForTheMenu() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        review.onRowAction("Revert", _ -> {
        });
        final @NotNull JTable table = theTable(review);
        final @NotNull Point onTheChange = new Point(10, table.getCellRect(0, 0, true).y + table.getRowHeight() / 2);

        released(table, new MouseEvent(aWindowOnScreen(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, onTheChange.x, onTheChange.y, 1, true, MouseEvent.BUTTON3));
        Arrays.stream(Window.getWindows()).flatMap(window -> UIUtil.uiTraverser(window).filter(JPopupMenu.class).toList().stream()).forEach(menu -> menu.setVisible(false));

        assertEquals("a right click on an unticked change did not tick it beside the marker", List.of(0, 1), review.getSelectedRows());
    }

    public void testOnlyARightClickOnATableWithARowMenuChangesTheTicks() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        final @NotNull JTable table = theTable(review);
        final @NotNull Point onTheChange = new Point(10, table.getCellRect(0, 0, true).y + table.getRowHeight() / 2);

        released(table, rightClickedAt(table, onTheChange));
        assertEquals("a right click on a table with no row menu ticked the row", List.of(1), review.getSelectedRows());

        review.onRowAction("Revert", _ -> {
        });
        released(table, new MouseEvent(table, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, onTheChange.x, onTheChange.y, 1, false, MouseEvent.BUTTON1));
        assertEquals("a left click's release ticked the row", List.of(1), review.getSelectedRows());
    }

    // Rule-SHARE-129
    public void testPointingBelowTheRowsSaysNothing() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        final @NotNull JTable table = theTable(review);

        assertNull("pointing below the rows said something", table.getToolTipText(pointedAt(table, new Point(10, table.getRowHeight() * 5))));
    }

    public void testSelectingAllOfAnEmptyTableTicksNothing() {
        final @NotNull SelectionTable empty = new SelectionTable(List.of("Change", "Where"), List.of(20, 20));

        empty.selectAll();

        assertEquals("selecting all of an empty table ticked a row", List.of(), empty.getSelectedRows());
    }

    public void testTakingAwayTheMarkerRowLeavesNothingFixed() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();

        review.removeRow(1);

        assertEquals("the change left after the marker went is ticked", List.of(), review.getSelectedRows());
        assertEquals("the marker row was not taken away", 1, review.getRowCount());
    }

    // Rule-INTERNAL-144
    public void testAWidthGivenForAColumnTheTableDoesNotHaveIsPassedOver() {
        final @NotNull SelectionTable review = new SelectionTable(List.of("Change"), List.of(20, 30));
        final @NotNull JTable table = theTable(review);

        assertEquals("the one column is not twenty characters wide", 20 * table.getFontMetrics(table.getFont()).charWidth('m'), table.getColumnModel().getColumn(0).getPreferredWidth());
    }

    // Rule-SHARE-129
    public void testARightClickBelowTheRowsUnticksTheChangesButNotTheMarker() {
        final @NotNull SelectionTable review = aReviewOfOneChangeAndOneMarker();
        review.onRowAction("Revert", _ -> {
        });
        final @NotNull JTable table = theTable(review);
        review.selectAll();

        final @NotNull MouseEvent below = rightClickedAt(table, new Point(10, table.getRowHeight() * 5));
        for (final MouseListener listener : table.getMouseListeners()) listener.mouseReleased(below);

        assertEquals("a right click below the rows did not untick the change, or unticked the marker", List.of(1), review.getSelectedRows());
    }
}
