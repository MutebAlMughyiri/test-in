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
import org.jetbrains.annotations.NotNull;
import org.testin.TestinLog;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Optional;

public class BreadcrumbStepsIdeTest extends AbstractViewPanelIdeTest {

    private static void click(final @NotNull Component step) {
        final @NotNull MouseEvent clicked = new MouseEvent(step, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : step.getMouseListeners()) listener.mouseClicked(clicked);
    }

    // Rule-VIEW-PANEL-042
    public void testEveryStepGoesToThePlaceItNamesAndBringsUpTheTree() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        final @NotNull TestCaseDto testCase = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), testCase, Optional.empty(), testSet.treePath());

        final @NotNull List<String> container = TestinLog.during(() -> click(Drawn.reading(tab, "Test Cases")));
        assertEquals("the tree did not come up on the step", 1, view.timesTheTreeCameUp());
        assertTrue("the step did not go to the place it names: " + container, container.stream().anyMatch(line -> line.contains("Going to Test Cases in NAFATH > Test Cases")));

        final @NotNull List<String> project = TestinLog.during(() -> click(Drawn.reading(tab, "NAFATH")));
        assertEquals("the tree did not come up on the step", 2, view.timesTheTreeCameUp());
        assertTrue("the step did not go to the place it names: " + project, project.stream().anyMatch(line -> line.contains("Going to NAFATH in NAFATH")));
    }
}
