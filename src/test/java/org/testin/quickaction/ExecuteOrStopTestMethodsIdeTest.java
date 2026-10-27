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

package org.testin.quickaction;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.actions.TestinDataKeys;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.runner.AutomationExecution;
import org.testin.runner.CapturedRunner;
import org.testin.services.Services;

import java.util.List;

import static org.junit.Assert.assertNotEquals;

public class ExecuteOrStopTestMethodsIdeTest extends AbstractCodegenIdeTest {

    private @NotNull AnActionEvent theMenuEntryOn(final @NotNull List<TestCase> selected) {
        final @NotNull AnAction run = ActionManager.getInstance().getAction("Testin.RunTestMethod");
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(run, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, selected)
                .build());
        ActionUtil.updateAction(run, e);
        assertTrue("Run Test Method is gray: " + e.getPresentation().getDescription(), e.getPresentation().isEnabled());
        return e;
    }

    private void pressed(final @NotNull List<TestCase> selected) {
        ActionUtil.performAction(ActionManager.getInstance().getAction("Testin.RunTestMethod"), theMenuEntryOn(selected));
    }

    // UC-CODEGEN-008, UC-CODEGEN-009, Rule-CODEGEN-031, Rule-CODEGEN-036
    public void testTheMenuEntryRunsTheSelectedTestCasesAsOneExecutionAndThenStopsThem() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCase> selected = List.of(createdTestCase(login, "Log in with a valid user", "a"), createdTestCase(login, "Log out", "b"));
        final @NotNull CapturedRunner runner = CapturedRunner.installed(getTestRootDisposable());
        final @NotNull AutomationExecution execution = Services.getInstance(getProject(), AutomationExecution.class);
        final @NotNull String runText = String.valueOf(theMenuEntryOn(selected).getPresentation().getText());

        pressed(selected);

        Await.until("the menu entry ran nothing", () -> !runner.executions().isEmpty());
        assertEquals("the selected test cases were not one execution", List.of(selected), runner.executions());
        assertNotEquals("the menu entry did not become the stop while its test cases run", runText, String.valueOf(theMenuEntryOn(selected).getPresentation().getText()));

        pressed(selected);

        assertFalse("the stop left a test case running", selected.stream().anyMatch(testCase -> execution.isRunning(testCase.getUuid())));
    }
}
