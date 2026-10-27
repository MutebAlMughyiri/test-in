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

import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.card.AbstractCard;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.quickaction.TestCaseQuickAction;
import org.testin.runner.CapturedRunner;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CardHoverIdeTest extends AbstractCodegenIdeTest {

    private @NotNull CapturedRunner runner = new CapturedRunner();

    @Override
    protected void setUp() {
        super.setUp();
        runner = CapturedRunner.installed(getTestRootDisposable());
    }

    private @NotNull CardList cardsOf(final @NotNull TestSetNode testSet) {
        final @NotNull CardList list = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable()).getList();
        list.setSize(900, 2000);
        return list;
    }

    private static @NotNull CardMouseListener listenerOf(final @NotNull CardList list) {
        return Arrays.stream(list.getMouseMotionListeners()).filter(CardMouseListener.class::isInstance).map(CardMouseListener.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("the cards have no mouse listener"));
    }

    private static @NotNull Point onRun(final @NotNull CardList list) {
        final @NotNull Rectangle cell = list.getCellBounds(0, 0);
        final @NotNull AbstractCard card = (AbstractCard) list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(0), 0, false, false);
        return IntStream.range(0, cell.width * cell.height)
                .mapToObj(i -> new Point(i % cell.width, i / cell.width))
                .filter(at -> card.actionAt(at).filter(offered -> offered.action() == TestCaseQuickAction.RUN_TEST_METHOD).isPresent())
                .findFirst()
                .map(at -> new Point(cell.x + at.x, cell.y + at.y))
                .orElseThrow(() -> new AssertionError("the card shows no Run button"));
    }

    private static @NotNull Point offEveryButton(final @NotNull CardList list) {
        final @NotNull Rectangle cell = list.getCellBounds(0, 0);
        return new Point(cell.x + cell.width - 2, cell.y + cell.height - 2);
    }

    private static @NotNull MouseEvent moved(final @NotNull CardList list, final @NotNull Point at) {
        return new MouseEvent(list, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, at.x, at.y, 0, false, MouseEvent.NOBUTTON);
    }

    private static @NotNull MouseEvent pressed(final @NotNull CardList list, final @NotNull Point at) {
        return new MouseEvent(list, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, at.x, at.y, 1, false, MouseEvent.BUTTON1);
    }

    private static @NotNull MouseEvent exited(final @NotNull CardList list) {
        return new MouseEvent(list, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false, MouseEvent.NOBUTTON);
    }

    // UC-EDITOR-PANEL-043
    public void testHoveringRunShowsTheHandAndWhatItDoesAndPressingItRunsTheTestCase() {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        final @NotNull TestCase testCase = createdTestCase(testSet, "Log in with a valid user", "m0001");
        settled();
        final @NotNull CardList list = cardsOf(testSet);
        final @NotNull CardMouseListener listener = listenerOf(list);
        final @NotNull Point run = onRun(list);

        listener.mouseMoved(moved(list, run));

        assertEquals("hovering a card does not mark it hovered", 0, list.getHoveredIndex());
        assertEquals("hovering Run does not mark Run hovered", TestCaseQuickAction.RUN_TEST_METHOD.name(), list.getHoveredIconAction());
        assertEquals("hovering Run does not show the hand", Cursor.HAND_CURSOR, list.getCursor().getType());
        assertTrue("hovering Run does not say what it does: " + Drawn.hovering(list), Drawn.hovering(list).startsWith(Bundle.message("action.Testin.RunTestMethod.text")));

        final @NotNull MouseEvent press = pressed(list, run);
        listener.mousePressed(press);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("pressing Run did not run the test case", List.of(List.of(testCase)), runner.executions());
        assertTrue("pressing Run left the press to the list as well", press.isConsumed());
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-301
    public void testPressingRunOnADisabledTestCaseRunsNothingAndSaysWhy() {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        createdTestCase(testSet, "Log in with a valid user", "m0001").setStatus(TestCaseStatus.DISABLED);
        settled();
        final @NotNull CardList list = cardsOf(testSet);
        final @NotNull CardMouseListener listener = listenerOf(list);
        final @NotNull Point run = onRun(list);
        final @NotNull String why = Bundle.message("run.test.method.disabled", TestCaseStatus.DISABLED.getLabel());
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        listener.mouseMoved(moved(list, run));
        assertEquals("hovering a gray Run does not say why it is gray", why, Drawn.hovering(list));

        listener.mousePressed(pressed(list, run));

        assertEquals("pressing a gray Run ran the test case", List.of(), runner.executions());
        assertEquals("pressing a gray Run does not say why", List.of(why), said.shown());
    }

    // UC-EDITOR-PANEL-043
    public void testOffTheButtonsTheArrowComesBackAPressRunsNothingAndLeavingClearsTheHover() {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        createdTestCase(testSet, "Log in with a valid user", "m0001");
        settled();
        final @NotNull CardList list = cardsOf(testSet);
        final @NotNull CardMouseListener listener = listenerOf(list);
        listener.mouseMoved(moved(list, onRun(list)));

        final @NotNull Point off = offEveryButton(list);
        listener.mouseMoved(moved(list, off));

        assertEquals("the card stopped being hovered while the pointer was still on it", 0, list.getHoveredIndex());
        assertEquals("a button stayed hovered after the pointer left it", "", list.getHoveredIconAction());
        assertEquals("the hand stayed after the pointer left the button", Cursor.DEFAULT_CURSOR, list.getCursor().getType());

        final @NotNull MouseEvent press = pressed(list, off);
        listener.mousePressed(press);
        assertEquals("a press off every button ran the test case", List.of(), runner.executions());
        assertFalse("a press off every button was kept from the list", press.isConsumed());

        listener.mouseExited(exited(list));

        assertEquals("leaving the cards left one hovered", -1, list.getHoveredIndex());
        assertEquals("leaving the cards left its tooltip", "", Drawn.hovering(list));
    }
}
