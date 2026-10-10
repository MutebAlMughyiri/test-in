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

package org.testin.runner;

import com.intellij.execution.ExecutionManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.TreeGesture;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Refused;
import org.testin.services.OptionalPlugin;

import java.util.List;
import java.util.Objects;

public class ExecuteTestsFromTheTreeIdeTest extends AbstractCodegenIdeTest {

    private static @NotNull AnAction runTests() {
        return Objects.requireNonNull(ActionManager.getInstance().getAction("Testin.RunTests"), "Run Tests is not registered");
    }

    // Rule-TREE-PANEL-078
    public void testANodeWithNoTestCasesSaysSoAndRunsNothing() {
        final @NotNull TestSetNode empty = createdTestSet("Empty");
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        TreeGesture.pressed(getProject(), runTests(), List.of(empty));

        final @NotNull List<String> said = balloons.shown();
        assertTrue("running a node with no test cases did not say so: " + said, said.contains(Refused.NOTHING_TO_RUN.about("Empty")));
        assertEquals("running a node with no test cases started something", 0, ExecutionManager.getInstance(getProject()).getRunningProcesses().length);
    }

    // Rule-TREE-PANEL-079
    public void testWithoutTestNgRunStaysOnTheMenuGrayAndSaysWhatItNeeds() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        indexedTestCase(login, "Log in with a valid user", "m");
        assertTrue("Run is gray with the TestNG plugin present", TreeGesture.updated(getProject(), runTests(), List.of(login)).isEnabled());

        OptionalPlugin.TESTNG.missingUntil(getTestRootDisposable());
        final @NotNull Presentation shown = TreeGesture.updated(getProject(), runTests(), List.of(login));

        assertTrue("Run left the menu without the TestNG plugin", shown.isVisible());
        assertFalse("Run is not gray without the TestNG plugin", shown.isEnabled());
        assertEquals(OptionalPlugin.TESTNG.needs(Objects.requireNonNullElse(runTests().getTemplatePresentation().getText(), "")), shown.getText());
        assertTrue("Run does not read (needs the TestNG plugin): " + shown.getText(), Objects.requireNonNullElse(shown.getText(), "").endsWith("(needs the TestNG plugin)"));
    }
}
