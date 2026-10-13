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
package org.testin.editor.testrun;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.editor.EditorFixtures;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.card.CardTitle;
import org.testin.editor.card.Offered;
import org.testin.editor.card.Slot;
import org.testin.editor.open.TestinEditors;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.runner.CapturedRunner;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testrun.TestRunFixture;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JTextArea;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

public class ExecutingDotIdeTest extends AbstractCodegenIdeTest {

    @Override
    protected void setUp() {
        super.setUp();
        CapturedRunner.installed(getTestRootDisposable());
    }

    private @NotNull List<TestCaseDto> profileTestCases() {
        final @NotNull TestSetNode profile = createdTestSet("Profile");
        final @NotNull List<TestCaseDto> made = List.of(
                createdTestCase(profile, "Change the profile photo", "changeTheProfilePhoto"),
                createdTestCase(profile, "Change the mobile number", "changeTheMobileNumber"));
        settled();
        return made;
    }

    private boolean executingNow(final @NotNull TestCaseDto testCase) {
        return ExecutingNow.is(getProject(), testCase.getId());
    }

    private static @NotNull List<Offered> runAndNavigate() {
        return List.of(new Offered(TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD, Optional.empty()), new Offered(TestCaseQuickAction.RUN_TEST_METHOD, Optional.empty()));
    }

    // Rule-EDITOR-PANEL-291
    public void testTheWalkedTestCaseIsExecutingAndTheOneItLeftIsNot() {
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunInTheIde(getProject(), EditorFixtures.testRunOver(getProject(), root, profileTestCases()));
        try {
            final @NotNull List<TestCaseDto> shown = editor.getCurrentTestCases();
            assertFalse("a test case shows as executing before anything started", executingNow(shown.getFirst()));

            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
            assertTrue("the walked test case does not show as executing", executingNow(shown.getFirst()));

            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.PASSED), editor.getList());
            Await.until("the walk did not move on", () -> editor.getWalk().getCurrentlyExecutingIndex() == 1);
            assertFalse("the test case the walk left still shows as executing", executingNow(shown.getFirst()));
            assertTrue("the test case the walk moved to does not show as executing", executingNow(shown.get(1)));

            editor.onStopExecutionClicked();
            assertFalse("a stopped walk still shows its test case as executing", executingNow(shown.get(1)));
        } finally {
            Services.getInstance(getProject(), TestinEditors.class).closeAll();
        }
    }

    // Rule-EDITOR-PANEL-291
    public void testATestCaseWaitingForTheAutomationShowsNoDot() {
        final @NotNull List<TestCaseDto> testCases = profileTestCases();
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRunOver(getProject(), root, testCases), getTestRootDisposable());
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            assertTrue("the run button did not hand the test case over", Services.getInstance(getProject(), TestNGExecution.class).isRunning(testCases.getFirst().getId()));

            assertFalse("a test case TestNG has not started yet shows as executing", executingNow(testCases.getFirst()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-291
    public void testTheHoverIconsStartAfterTheDot() {
        final @NotNull Rectangle dot = CardTitle.executingDot(200);
        final @NotNull List<Slot> slots = CardTitle.descriptionActionIcons(200, runAndNavigate(), true).slots();

        assertTrue("a hover icon sits on the dot", slots.stream().noneMatch(slot -> slot.at().intersects(dot)));
        assertEquals("the dot is not where the first hover icon sits without it", CardTitle.descriptionActionIcons(200, runAndNavigate(), false).slots().getFirst().at(), dot);
        assertTrue("the title column does not make room for the dot", CardTitle.titleColumnWidth(900, CardTitle.slotCount(runAndNavigate(), true)) < CardTitle.titleColumnWidth(900, CardTitle.slotCount(runAndNavigate(), false)));
    }

    // Rule-VIEW-PANEL-125
    public void testTheViewPanelTitleSaysExecutingOnlyWhileItIs() {
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunInTheIde(getProject(), EditorFixtures.testRunOver(getProject(), root, profileTestCases()));
        try {
            final @NotNull TestCaseDto first = editor.getCurrentTestCases().getFirst();
            assertFalse("the title says executing before anything started", titleSaysExecuting(first));

            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);

            assertTrue("the title of an executing test case does not say so", titleSaysExecuting(first));
        } finally {
            editor.onStopExecutionClicked();
            Services.getInstance(getProject(), TestinEditors.class).closeAll();
        }
    }

    private boolean titleSaysExecuting(final @NotNull TestCaseDto testCase) {
        return Drawn.components(Drawn.detailsTab(getProject(), testCase, Optional.empty(), List.of())).stream()
                .filter(JTextArea.class::isInstance)
                .anyMatch(area -> Bundle.message("execution.executing").equals(area.getAccessibleContext().getAccessibleDescription()));
    }
}
