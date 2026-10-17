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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.TreeGesture;
import org.testin.editor.EditorFixtures;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.toolbar.StartExecutionButton;
import org.testin.editor.toolbar.StopExecutionButton;
import org.testin.model.TestCase;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.runner.CapturedRunner;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Objects;

public class ExecutionModeIdeTest extends AbstractCodegenIdeTest {

    private @NotNull CapturedRunner runner = new CapturedRunner();

    private static @NotNull String tooltipOf(final @NotNull StartExecutionButton start) {
        return Objects.requireNonNullElse(start.getAccessibleContext().getAccessibleName(), "");
    }

    private static @NotNull AnAction runTests() {
        return Objects.requireNonNull(ActionManager.getInstance().getAction("Testin.RunTests"), "Run Tests is not registered");
    }

    @Override
    protected void setUp() {
        super.setUp();
        runner = CapturedRunner.installed(getTestRootDisposable());
    }

    private @NotNull List<TestCase> loginTestCases() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCase> made = List.of(
                createdTestCase(login, "Log in with Nafath", "logInWithNafath"),
                createdTestCase(login, "Log in with a wrong password", "logInWithAWrongPassword"),
                createdTestCase(login, "Log out from the profile page", "logOutFromTheProfilePage"));
        settled();
        return made;
    }

    // Rule-EDITOR-PANEL-290
    public void testASingleRunButtonLeavesStartExecutionAvailable() {
        final @NotNull List<TestCase> testCases = loginTestCases();
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRunOver(getProject(), root, testCases), getTestRootDisposable());
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            assertTrue("the run button did not start its test case", Services.getInstance(getProject(), TestNGExecution.class).isRunning(testCases.getFirst().getUuid()));

            assertEquals("a single run button put the test run in a mode", ExecutionMode.IDLE, editor.getWalk().mode());
            final @NotNull StartExecutionButton start = editor.getToolBar().getToolbarItem(StartExecutionButton.class);
            assertTrue("a single run button hid Start Execution", start.isVisible());
            assertTrue("a single run button grayed Start Execution", start.isEnabled());
            assertFalse("a single run button showed Stop Execution", editor.getToolBar().getToolbarItem(StopExecutionButton.class).isVisible());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-290
    public void testRunTestsGraysStartExecutionWithItsReasonAndStopExecutionEndsIt() {
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRunOver(getProject(), root, loginTestCases()), getTestRootDisposable());
        try {
            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("Run Tests did not put the test run in automation", ExecutionMode.AUTOMATION, editor.getWalk().mode());

            final @NotNull StartExecutionButton start = editor.getToolBar().getToolbarItem(StartExecutionButton.class);
            assertTrue("Start Execution left the toolbar while Run Tests executes", start.isVisible());
            assertFalse("Start Execution is not gray while Run Tests executes", start.isEnabled());
            assertEquals(Bundle.message("execution.mode.automation"), tooltipOf(start));
            assertTrue("Stop Execution is not offered while Run Tests executes", editor.getToolBar().getToolbarItem(StopExecutionButton.class).isVisible());

            editor.onStopExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("Stop Execution did not end Run Tests", ExecutionMode.IDLE, editor.getWalk().mode());
            assertTrue("Start Execution is still gray after Run Tests stopped", start.isEnabled());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-290
    public void testRunTestsDuringTheWalkRunsNothingAndSaysWhy() {
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRunOver(getProject(), root, loginTestCases()), getTestRootDisposable());
        try {
            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().mode() == ExecutionMode.WALK);

            final @NotNull List<String> said = Said.during(getProject(), () -> {
                editor.runWhenLoaded();
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            });

            assertEquals("Run Tests started during the walk", List.of(), runner.executions());
            assertTrue("Run Tests during the walk did not say why: " + said, said.contains(Bundle.message("execution.mode.walk")));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-TREE-PANEL-141
    public void testRunTestsInTheTreeIsGrayWithItsReasonWhileTheWalkIsOn() {
        final @NotNull TestRunNode testRun = EditorFixtures.testRunOver(getProject(), root, loginTestCases());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunInTheIde(getProject(), testRun);
        try {
            assertTrue("Run Tests is gray on an idle test run", TreeGesture.updated(getProject(), runTests(), List.of(testRun)).isEnabled());

            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().mode() == ExecutionMode.WALK);

            final @NotNull Presentation shown = TreeGesture.updated(getProject(), runTests(), List.of(testRun));
            assertFalse("Run Tests is not gray while the walk is on", shown.isEnabled());
            assertEquals(Bundle.message("execution.mode.walk"), shown.getDescription());
        } finally {
            editor.onStopExecutionClicked();
            Services.getInstance(getProject(), TestinEditors.class).closeAll();
        }
    }
}
