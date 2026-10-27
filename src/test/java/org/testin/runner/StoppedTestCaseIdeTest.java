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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.model.TestCase;
import org.testin.model.status.AutomationExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testin.services.Services;

import javax.swing.Icon;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class StoppedTestCaseIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-038
    public void testATestCaseTheTesterStoppedIsRecordedAsNotRunNeverAsFailed() {
        final @NotNull TestCase testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        AutomationExecutionRecorder.startRecording(getProject());
        final @NotNull AutomationExecution execution = Services.getInstance(getProject(), AutomationExecution.class);

        execution.starting(testCase);
        execution.stop(List.of(testCase));
        AutomationExecutionReported.broadcast(getProject(), testCase.getUuid().toString(), AutomationExecutionStatus.FAILED, Duration.ZERO, Failure.NONE);
        settled();

        assertEquals("the killed process reported the stopped test case as failed, and it was recorded so", AutomationExecutionStatus.IDLE, execution.automationExecutionStatusOf(testCase));
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037, Rule-CODEGEN-034
    public void testAStopPressedWhileTheIdeStillBuildsEndsTheProcessAsItStarts() {
        final @NotNull TestCase testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull AutomationExecution execution = Services.getInstance(getProject(), AutomationExecution.class);
        final @NotNull String testRunName = execution.freeConfigurationName("LoginTest");
        execution.starting(testCase);
        execution.launched(List.of(testCase), testRunName);

        execution.stop(List.of(testCase));

        final @NotNull ProcessHandler process = new NopProcessHandler();
        process.startNotify();
        getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarted(DefaultRunExecutor.EXECUTOR_ID, launchOf(testRunName), process);

        assertTrue("the process of an execution stopped while the IDE was still building was left running", Optional.ofNullable(process.getUserData(ProcessHandler.TERMINATION_REQUESTED)).orElse(false));
        assertTrue("the process of an execution stopped while the IDE was still building is not ending", process.isProcessTerminating() || process.isProcessTerminated());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public void testAStopPressedWhileTheIdeStillBuildsKillsAProcessThatCanBeKilled() {
        final @NotNull TestCase testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull AutomationExecution execution = Services.getInstance(getProject(), AutomationExecution.class);
        final @NotNull String testRunName = execution.freeConfigurationName("LoginTest");
        execution.starting(testCase);
        execution.launched(List.of(testCase), testRunName);
        execution.stop(List.of(testCase));
        final @NotNull GeneralCommandLine waiting = SystemInfo.isWindows ? new GeneralCommandLine("ping", "-n", "60", "127.0.0.1") : new GeneralCommandLine("sleep", "60");
        final @NotNull KillableProcessHandler process;
        try {
            process = new KillableProcessHandler(waiting);
        } catch (final ExecutionException ex) {
            throw new AssertionError("could not start a process to stop: " + ex.getMessage(), ex);
        }
        process.startNotify();

        getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarted(DefaultRunExecutor.EXECUTOR_ID, launchOf(testRunName), process);

        Await.until("the process of an execution stopped while the IDE was still building was not killed", process::isProcessTerminated);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-092
    public void testAProcessThatEndsWithoutReportingATestCaseTakesItOutOfRunning() {
        final @NotNull TestCase testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull AutomationExecution execution = Services.getInstance(getProject(), AutomationExecution.class);
        final @NotNull String testRunName = execution.freeConfigurationName("LoginTest");
        execution.starting(testCase);
        execution.launched(List.of(testCase), testRunName);
        final @NotNull ExecutionEnvironment launch = launchOf(testRunName);
        final @NotNull ProcessHandler process = new NopProcessHandler();
        process.startNotify();
        getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarted(DefaultRunExecutor.EXECUTOR_ID, launch, process);
        assertTrue("the test case is not running while its process runs", execution.isRunning(testCase.getUuid()));

        getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processTerminated(DefaultRunExecutor.EXECUTOR_ID, launch, process, 1);

        Await.until("a test case its ended process never reported was left running", () -> !execution.isRunning(testCase.getUuid()));
    }

    private @NotNull ExecutionEnvironment launchOf(final @NotNull String testRunName) {
        final @NotNull RunProfile profile = new RunProfile() {
            @Override
            public @NotNull RunProfileState getState(final @NotNull Executor executor, final @NotNull ExecutionEnvironment environment) {
                return (_, _) -> {
                    throw new ExecutionException("A stand-in launch runs nothing");
                };
            }

            @Override
            public @NotNull String getName() {
                return testRunName;
            }

            @Override
            public @NotNull Icon getIcon() {
                return AllIcons.RunConfigurations.TestState.Run;
            }
        };
        final @NotNull ProgramRunner<RunnerSettings> standIn = new ProgramRunner<>() {
            @Override
            public @NotNull String getRunnerId() {
                return "testin.runner.stopStandIn";
            }

            @Override
            public boolean canRun(final @NotNull String executorId, final @NotNull RunProfile runProfile) {
                return true;
            }

            @Override
            public void execute(final @NotNull ExecutionEnvironment environment) {
            }
        };
        return new ExecutionEnvironmentBuilder(getProject(), DefaultRunExecutor.getRunExecutorInstance()).runProfile(profile).runner(standIn).build();
    }
}
