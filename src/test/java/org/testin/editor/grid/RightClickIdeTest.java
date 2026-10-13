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

package org.testin.editor.grid;

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.editor.cardview.CardMouseListener;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewBtn;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.testcase.TestSetEditorFields;
import org.testin.view.Drawn;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class RightClickIdeTest extends AbstractTempRootIdeTest {

    private static boolean refusedForWantOfAScreen(final @NotNull Runnable press) {
        try {
            press.run();
            return false;
        } catch (final IllegalArgumentException menuWithNoScreen) {
            return true;
        }
    }

    private static void rightClick(final @NotNull Runnable press, final @NotNull BooleanSupplier movedFirst) {
        if (refusedForWantOfAScreen(press))
            assertTrue("the menu opened before the selection moved to what was clicked", movedFirst.getAsBoolean());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull TestSetEditor fourTestCases() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 4);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    // Rule-EDITOR-PANEL-110
    public void testRightClickingAGridRowOutsideTheSelectionMovesTheSelectionThereFirst() {
        final @NotNull TestSetEditor editor = fourTestCases();
        editor.getToolBar().getToolbarItem(GridViewBtn.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JBTable table = Drawn.components(editor.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow();
        table.setSize(1200, 600);
        table.setRowSelectionInterval(0, 1);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        final @NotNull Rectangle row = table.getCellRect(3, table.convertColumnIndexToView(TestSetEditorFields.DESCRIPTION.column()), true);
        final @NotNull MouseEvent press = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, (int) row.getCenterX(), (int) row.getCenterY(), 1, true, MouseEvent.BUTTON3);
        final @NotNull BooleanSupplier moved = () -> Arrays.equals(new int[]{3}, table.getSelectedRows());

        rightClick(() -> {
            for (final MouseListener listener : table.getMouseListeners())
                if (listener instanceof GridContextMenuListener) listener.mousePressed(press);
        }, moved);

        assertTrue("right-clicking outside the selection did not move it to the row clicked", moved.getAsBoolean());
        assertEquals("the cards did not follow the grid's selection", 3, editor.getList().getSelectedIndex());
    }

    // Rule-EDITOR-PANEL-110
    public void testRightClickingACardOutsideTheSelectionMovesTheSelectionThereFirst() {
        final @NotNull TestSetEditor editor = fourTestCases();
        final @NotNull JBList<TestCaseDto> list = editor.getList();
        list.setSize(900, 2000);
        list.setSelectionInterval(0, 1);

        final @NotNull Rectangle card = list.getCellBounds(3, 3);
        final @NotNull MouseEvent click = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, card.x + 5, card.y + card.height / 2, 1, true, MouseEvent.BUTTON3);
        final @NotNull BooleanSupplier moved = () -> List.of(3).equals(Arrays.stream(list.getSelectedIndices()).boxed().toList());

        rightClick(() -> {
            for (final MouseListener listener : list.getMouseListeners())
                if (listener instanceof CardMouseListener) listener.mouseClicked(click);
        }, moved);

        assertTrue("right-clicking a card outside the selection did not move it to the card clicked", moved.getAsBoolean());
    }
}
