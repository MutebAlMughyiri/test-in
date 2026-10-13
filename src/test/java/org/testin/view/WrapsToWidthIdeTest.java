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
package org.testin.view;

import com.intellij.ui.components.ActionLink;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.List;

public class WrapsToWidthIdeTest extends AbstractViewPanelIdeTest {
    private static final @NotNull String LONG = "Invocation 4, the 2nd fails - expect stay through 4 invocations, then Failed, and the account stays locked for the whole test run.";

    private static void layOut(final @NotNull JComponent pane, final int width) {
        pane.setSize(width, 900);
        for (int pass = 0; pass < 4; pass++)
            UIUtil.uiTraverser(pane).filter(Container.class).forEach(Container::doLayout);
    }

    private static int widestValueAt(final @NotNull JComponent pane, final int width) {
        layOut(pane, width);
        return UIUtil.uiTraverser(pane).filter(JTextArea.class).filter(area -> area.getText().equals(LONG)).map(JTextArea::getWidth).reduce(0, Math::max);
    }

    private static boolean fullyVisibleIn(final @NotNull Component part, final @NotNull Component pane) {
        @NotNull Rectangle shown = new Rectangle(part.getSize());
        for (Component at = part; at.getParent() != null && SwingUtilities.isDescendingFrom(at.getParent(), pane); at = at.getParent()) {
            shown.translate(at.getX(), at.getY());
            shown = shown.intersection(new Rectangle(at.getParent().getSize()));
        }
        return part.getWidth() > 0 && shown.width == part.getWidth() && shown.height == part.getHeight();
    }

    // Rule-VIEW-PANEL-124
    public void testANarrowerPanelWrapsTheValuesInsteadOfCuttingThemOff() {
        final @NotNull TestCaseDto testCase = aTestCase(aTestSet("Login"), LONG, "1");
        testCase.setExpectedResult(LONG);
        view.getPanel().show(List.of(testCase), List.of("Demo", "Test Cases", "Login"));
        settled();

        final @NotNull JComponent pane = ViewTab.DETAILS.paneOf(view.getPanel());
        final int wide = widestValueAt(pane, 1400);
        final int narrow = widestValueAt(pane, 420);

        assertTrue("the values were never drawn: " + wide, wide > 420);
        assertTrue("a value kept its width when the panel narrowed: " + wide + " then " + narrow, narrow < 420);
    }

    // Rule-VIEW-PANEL-124
    public void testANarrowerPanelMovesTheBugChipAndItsLinkToASecondRowRatherThanHidingThem() {
        final @NotNull TestCaseDto testCase = aTestCase(aTestSet("Transfers"), "No retry, fails (transfer) - expect Failed at once.", "1");
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().id(testCase.getId())
                .executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The transfer went through although it was above the balance").build())).build()));
        view.getPanel().show(List.of(testCase), testRun.getPath2());
        settled();

        final @NotNull JComponent pane = ViewTab.DETAILS.paneOf(view.getPanel());
        layOut(pane, 300);

        final @NotNull ActionLink report = UIUtil.uiTraverser(pane).filter(ActionLink.class).toList().stream()
                .filter(link -> Bundle.message("bug.dialog.title").equals(link.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the failed run item offers no Report Bug"));
        assertTrue("Report Bug was cut off when the panel narrowed", fullyVisibleIn(report, pane));
    }

    private static boolean drawn(final @NotNull Component part, final @NotNull Component pane) {
        for (Component at = part; at != null && !at.equals(pane); at = at.getParent()) {
            if (!at.isVisible()) return false;
        }
        return true;
    }

    private static @NotNull List<String> cutOff(final @NotNull JComponent pane, final int width) {
        layOut(pane, width);
        return UIUtil.uiTraverser(pane).filter(JLabel.class).toList().stream()
                .filter(label -> drawn(label, pane) && (label.getIcon() != null || !label.getText().isBlank()))
                .filter(label -> !fullyVisibleIn(label, pane))
                .map(label -> "'" + label.getText() + "' at " + width)
                .toList();
    }

    // Rule-VIEW-PANEL-124
    public void testTheBadgesAndIconsStayWholeAndTheLayoutIsTheSameEveryTime() {
        final @NotNull TestCaseDto testCase = aTestCase(aTestSet("Card Management"), "Retry 2, fails all 3 attempts - expect stay through 2 retries, then Failed.", "1");
        testCase.setPriority(Priority.HIGH);
        testCase.setGroups(List.of("SMOKE"));
        view.getPanel().show(List.of(testCase), List.of("test-02", "Test Cases", "Card Management"));
        settled();

        final @NotNull JComponent pane = ViewTab.DETAILS.paneOf(view.getPanel());
        final @NotNull JComponent content = (JComponent) ((JScrollPane) pane).getViewport().getView();

        assertEquals("a badge or an icon was cut off on a wide panel", List.of(), cutOff(pane, 1400));
        final int wide = content.getPreferredSize().height;
        assertEquals("a badge or an icon was cut off on a narrow panel", List.of(), cutOff(pane, 300));
        assertEquals("a badge, an icon or the breadcrumb was cut off on a panel narrower than the badges together", List.of(), cutOff(pane, 200));
        assertEquals("a badge or an icon was cut off on a wide panel again", List.of(), cutOff(pane, 1400));

        assertEquals("the same width laid out a different panel the second time", wide, content.getPreferredSize().height);
    }

    // Rule-VIEW-PANEL-124
    public void testTheHistoryTabScrollsSidewaysInsteadOfWrapping() {
        final @NotNull JScrollPane history = ViewTab.HISTORY.paneOf(view.getPanel());

        assertFalse("the History tab follows the panel's width, so a long card line cannot be scrolled to",
                history.getViewport().getView() instanceof final Scrollable scrollable && scrollable.getScrollableTracksViewportWidth());
    }
}
