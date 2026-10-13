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

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.actions.TestinDataKeys;
import org.testin.codegen.CodeChangeType;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class OneExecutionIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private final @NotNull List<RunProfile> launched = new ArrayList<>();

    private final @NotNull List<TestCaseDto> involved = new ArrayList<>();

    private @NotNull List<String> balloons = List.of();

    @Override
    protected void setUp() {
        super.setUp();
        balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
    }

    @Override
    protected void tearDown() {
        try {
            execution().stop(involved);
            final @NotNull RunManager runs = RunManager.getInstance(getProject());
            runs.getAllSettings().stream().filter(settings -> settings.getConfiguration() instanceof TestNGConfiguration).toList().forEach(runs::removeConfiguration);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull TestNGExecution execution() {
        return Services.getInstance(getProject(), TestNGExecution.class);
    }

    private void nothingIsReallyStarted() {
        final @NotNull ProgramRunner<RunnerSettings> standIn = new ProgramRunner<>() {
            @Override
            public @NotNull String getRunnerId() {
                return "testin.runner.standIn";
            }

            @Override
            public boolean canRun(final @NotNull String executorId, final @NotNull RunProfile profile) {
                return true;
            }

            @Override
            public void execute(final @NotNull ExecutionEnvironment environment) {
                launched.add(environment.getRunProfile());
            }
        };
        ExtensionTestUtil.maskExtensions(ProgramRunner.PROGRAM_RUNNER_EP, List.of(standIn), getTestRootDisposable());
    }

    private @NotNull List<TestCaseDto> theTestCases(final @NotNull List<TestCaseDto> testCases) {
        involved.addAll(testCases);
        return testCases;
    }

    private @NotNull String running(final int howMany) {
        return Done.counted(Bundle.message("execution.status.running"), howMany);
    }

    private @NotNull AnActionEvent theRunEntryFor(final @NotNull TestCaseDto testCase) {
        final @NotNull AnAction action = Optional.ofNullable(ActionManager.getInstance().getAction("Testin.RunTestMethod")).orElseThrow(() -> new AssertionError("Run Test Method is not registered"));
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, List.of(testCase))
                .build());
        ActionUtil.updateAction(action, e);
        return e;
    }

    // Rule-CODEGEN-097
    public void testAnExecutionAcrossModulesStartsNothingAndNamesThem() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCaseDto> selected = theTestCases(List.of(createdTestCase(login, "Log in", "b"), createdTestCase(login, "Log out", "c")));
        selected.forEach(execution()::starting);

        execution().acrossModules(selected, List.of("app", "api"));
        settled();

        assertTrue("the refusal does not name both modules: " + balloons, balloons.contains(Refused.ACROSS_MODULES.about("app, api")));
        assertTrue("a test case of the refused selection still reads as running", selected.stream().noneMatch(testCase -> execution().isRunning(testCase.getId())));
    }

    // Rule-CODEGEN-031
    public void testWhateverIsSelectedIsOneExecution() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCaseDto> selected = theTestCases(List.of(createdTestCase(login, "Log in", "b"), createdTestCase(login, "Log out", "c"), createdTestCase(createdTestSet("Payment"), "Pay", "b")));
        final @NotNull List<List<TestCaseDto>> handedOver = new ArrayList<>();
        ExtensionTestUtil.maskExtensions(AutomationExecutor.EP, List.of((_, testCases) -> handedOver.add(testCases)), getTestRootDisposable());

        ExecuteTestCases.run(getProject(), selected);

        assertEquals("the selection was split into more than one execution: " + handedOver, 1, handedOver.size());
        assertEquals("the one execution does not carry every selected test case", selected, handedOver.getFirst());
    }

    // Rule-CODEGEN-031, Rule-CODEGEN-033
    public void testOneMessageCountsTheTestCasesThatStartedOnceTheirMethodsAreFound() {
        nothingIsReallyStarted();
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCaseDto> selected = theTestCases(List.of(createdTestCase(login, "Log in", "b"), createdTestCase(login, "Log out", "c"), indexedTestCase(login, "Lock the account", "d")));
        settled();

        ExecuteTestCases.run(getProject(), selected);
        Await.until("the execution never started", () -> !launched.isEmpty());
        settled();

        assertEquals("the selection was not one execution: " + launched, 1, launched.size());
        assertEquals("the one execution does not run every test case that has a method", 2, ((TestNGConfiguration) launched.getFirst()).getPersistantData().getPatterns().size());
        final @NotNull List<String> runningSaid = balloons.stream().filter(said -> said.startsWith(running(1))).toList();
        assertEquals("the running message was not said once, counting the test cases that started: " + balloons, List.of(running(2)), runningSaid);
    }

    // Rule-CODEGEN-035
    public void testTheMethodsRunInTheOrderOfTheTestCasesInTheirTestSet() {
        nothingIsReallyStarted();
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto third = indexedTestCase(login, "Lock the account", "d");
        final @NotNull TestCaseDto first = indexedTestCase(login, "Log in", "b");
        final @NotNull TestCaseDto second = indexedTestCase(login, "Log out", "c");
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(third, first, second));
        settled();

        ExecuteTestCases.run(getProject(), theTestCases(List.of(third, second, first)));
        Await.until("the execution never started", () -> !launched.isEmpty());

        final @NotNull List<String> priorities = Stream.of(first, second, third)
                .map(testCase -> attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "priority"))
                .toList();
        assertEquals("TestNG is not told to run the methods in the order of the test cases in their test set", List.of("1", "2", "3"), priorities);
    }

    // Rule-CODEGEN-036
    public void testWhileATestCaseIsRunningTheRunButtonAndTheMenuEntryBecomeTheStop() {
        final @NotNull TestCaseDto testCase = theTestCases(List.of(createdTestCase(createdTestSet("Login"), "Log in", "b"))).getFirst();
        assertEquals("the menu entry is not the run before anything runs", Bundle.message("action.Testin.RunTestMethod.text"), theRunEntryFor(testCase).getPresentation().getText());

        execution().starting(testCase);

        assertEquals("the run button on the card did not become the stop", TestCaseQuickAction.STOP_TEST_METHOD, TestCaseQuickAction.onCard(getProject(), testCase.getParent(), testCase).get(1).action());
        assertEquals("the run button on the view panel did not become the stop", TestCaseQuickAction.STOP_TEST_METHOD, TestCaseQuickAction.RUN_TEST_METHOD.offer(getProject(), Optional.empty(), testCase).action());
        final @NotNull AnActionEvent menu = theRunEntryFor(testCase);
        assertEquals("the menu entry did not become the stop", Bundle.message("card.stop.test.method"), menu.getPresentation().getText());
        assertEquals("the menu entry still shows the run icon", TestCaseQuickAction.STOP_TEST_METHOD.getIcon(), menu.getPresentation().getIcon());
    }

    // Rule-CODEGEN-074
    public void testATestCaseThatCannotRunIsReportedOnceByDescriptionAndSeveralByCount() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto alone = indexedTestCase(login, "Lock the account", "b");
        settled();

        ExecuteTestCases.run(getProject(), theTestCases(List.of(alone)));
        Await.until("a test case with no method was never reported", () -> balloons.contains(Refused.NO_GENERATED_CODE.about(alone.getDescription())));

        final @NotNull List<TestCaseDto> several = theTestCases(List.of(indexedTestCase(login, "Unlock the account", "c"), indexedTestCase(login, "Reset the password", "d"), indexedTestCase(login, "Change the password", "e")));
        ExecuteTestCases.run(getProject(), several);
        Await.until("several test cases with no method were never reported", () -> balloons.contains(Refused.NO_GENERATED_CODE_COUNTED.about("3")));
        settled();

        final @NotNull List<String> shown = balloons;
        assertEquals("one test case with no method was not reported exactly once: " + shown, 1, shown.stream().filter(said -> said.equals(Refused.NO_GENERATED_CODE.about(alone.getDescription()))).count());
        assertEquals("several test cases with no method were reported more than once, or one by one: " + shown, 1, shown.stream().filter(said -> said.contains("no generated code")).count() - 1);
        assertTrue("an execution with nothing to run still said it was running: " + shown, shown.stream().noneMatch(said -> said.startsWith(running(1))));
    }
}
