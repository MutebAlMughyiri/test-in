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
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCase;
import org.testin.model.testrun.Failure;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.services.Services;

import javax.swing.Icon;
import java.time.Duration;
import java.util.List;

public class StoppedTestCaseIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-038
    public void testATestCaseTheTesterStoppedIsRecordedAsNotRunNeverAsFailed() {
        final @NotNull TestCase testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        TestNGExecutionRecorder.startRecording(getProject());
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);

        execution.starting(testCase);
        execution.stop(List.of(testCase));
        TestNGExecutionReported.broadcast(getProject(), testCase.getUuid().toString(), TestNGExecutionStatus.FAILED, Duration.ZERO, Failure.NONE);
        settled();

        assertEquals("the killed process reported the stopped test case as failed, and it was recorded so", TestNGExecutionStatus.IDLE, execution.testNGExecutionStatusOf(testCase));
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037, Rule-CODEGEN-034
    public void testAStopPressedWhileTheIdeStillBuildsEndsTheProcessAsItStarts() {
        final @NotNull TestCase testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);
        final @NotNull String testRunName = execution.freeConfigurationName("LoginTest");
        execution.starting(testCase);
        execution.launched(List.of(testCase), testRunName);

        execution.stop(List.of(testCase));

        final @NotNull ProcessHandler process = new NopProcessHandler();
        process.startNotify();
        getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarted(DefaultRunExecutor.EXECUTOR_ID, launchOf(testRunName), process);

        assertEquals("the process of an execution stopped while the IDE was still building was left running", Boolean.TRUE, process.getUserData(ProcessHandler.TERMINATION_REQUESTED));
        assertTrue("the process of an execution stopped while the IDE was still building is not ending", process.isProcessTerminating() || process.isProcessTerminated());
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
