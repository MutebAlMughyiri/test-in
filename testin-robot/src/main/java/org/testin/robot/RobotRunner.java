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

package org.testin.robot;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.runner.AutomationExecution;
import org.testin.runner.AutomationExecutor;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RobotRunner implements AutomationExecutor {
    // UC-CODEGEN-008, Rule-CODEGEN-074
    private static void start(final @NotNull Project project, final @NotNull List<TestCase> testCases, final @NotNull Map<UUID, RobotTest> tests) {
        final @NotNull AutomationExecution execution = Services.getInstance(project, AutomationExecution.class);
        execution.noGeneratedCode(testCases.stream().filter(testCase -> !tests.containsKey(testCase.getUuid())).toList());

        final @NotNull List<TestCase> running = execution.stillWanted(testCases.stream().filter(testCase -> tests.containsKey(testCase.getUuid())).toList());
        if (running.isEmpty()) return;

        final @NotNull RunnerAndConfigurationSettings settings = execution.configurationFor(running.stream().map(TestCase::getDescription).toList(), RobotTestsConfigurationType.getInstance());

        if (!(settings.getConfiguration() instanceof RobotTestsConfiguration configuration)) {
            TestinLogger.warn("'" + settings.getName() + "' is not a Robot Framework configuration, so the execution was not started");
            return;
        }

        configuration.toRun(running.stream().map(TestCase::getUuid).toList(), tests.values().stream().map(RobotTest::suite).map(VirtualFile::getPath).distinct().toList());
        execution.launch(running, settings);
    }

    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.ROBOT_FRAMEWORK;
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031, Rule-CODEGEN-120
    @Override
    public void run(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        if (testCases.isEmpty()) return;

        BackgroundWork.logged(ReadAction.nonBlocking(() -> RobotTests.of(project, testCases.stream().map(TestCase::getUuid).toList()))
                .inSmartMode(project)
                .expireWith(Services.getInstance(project, AutomationExecution.class))
                .finishOnUiThread(ModalityState.nonModal(), tests -> start(project, testCases, tests))
                .submit(AppExecutorUtil.getAppExecutorService()), "Preparing the Robot Framework run");
    }
}
