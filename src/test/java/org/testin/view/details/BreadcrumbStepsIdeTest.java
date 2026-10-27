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
package org.testin.view.details;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.TestinLog;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.Optional;

public class BreadcrumbStepsIdeTest extends AbstractViewPanelIdeTest {

    private static void click(final @NotNull Component step) {
        final @NotNull MouseEvent clicked = new MouseEvent(step, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : step.getMouseListeners()) listener.mouseClicked(clicked);
    }

    private static void pointer(final @NotNull Component step, final boolean entering) {
        final @NotNull MouseEvent moved = new MouseEvent(step, entering ? MouseEvent.MOUSE_ENTERED : MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 2, 2, 0, false, MouseEvent.NOBUTTON);
        for (final MouseListener listener : step.getMouseListeners()) {
            if (entering) listener.mouseEntered(moved);
            else listener.mouseExited(moved);
        }
    }

    // UC-VIEW-PANEL-010
    public void testAStepUnderThePointerTurnsToTheLinkColorAndUnderlinesItself() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), aTestCase(testSet, "Log in with a valid user", "a"), Optional.empty(), testSet.treePath());
        final @NotNull Component step = Drawn.reading(tab, "Test Cases");

        pointer(step, true);
        assertEquals("a step under the pointer is not in the link color", JBUI.CurrentTheme.Link.Foreground.ENABLED, step.getForeground());
        assertEquals("a step under the pointer is not underlined", TextAttribute.UNDERLINE_ON, step.getFont().getAttributes().get(TextAttribute.UNDERLINE));

        pointer(step, false);
        assertEquals("a step the pointer left kept the link color", JBUI.CurrentTheme.ContextHelp.FOREGROUND, step.getForeground());
        assertFalse("a step the pointer left kept its underline", TextAttribute.UNDERLINE_ON.equals(step.getFont().getAttributes().get(TextAttribute.UNDERLINE)));
    }

    // Rule-VIEW-PANEL-042
    public void testEveryStepGoesToThePlaceItNamesAndBringsUpTheTree() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        final @NotNull TestCase testCase = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), testCase, Optional.empty(), testSet.treePath());

        final @NotNull List<String> container = TestinLog.during(() -> click(Drawn.reading(tab, "Test Cases")));
        assertEquals("the tree did not come up on the step", 1, view.timesTheTreeCameUp());
        assertTrue("the step did not go to the place it names: " + container, container.stream().anyMatch(line -> line.contains("Going to Test Cases in NAFATH > Test Cases")));

        final @NotNull List<String> testProjectLog = TestinLog.during(() -> click(Drawn.reading(tab, "NAFATH")));
        assertEquals("the tree did not come up on the step", 2, view.timesTheTreeCameUp());
        assertTrue("the step did not go to the place it names: " + testProjectLog, testProjectLog.stream().anyMatch(line -> line.contains("Going to NAFATH in NAFATH")));
    }
}
