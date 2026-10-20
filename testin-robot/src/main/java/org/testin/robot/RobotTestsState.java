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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

// UC-CODEGEN-008, Rule-CODEGEN-120
final class RobotTestsState extends CommandLineState {
    private final @NotNull RobotTestsConfiguration configuration;

    RobotTestsState(final @NotNull ExecutionEnvironment environment, final @NotNull RobotTestsConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
    }

    // UC-CODEGEN-008, Rule-CODEGEN-120
    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        final @NotNull Project project = configuration.getProject();
        final @NotNull String interpreter = Optional.ofNullable(ProjectRootManager.getInstance(project).getProjectSdk())
                .map(Sdk::getHomePath)
                .orElseThrow(() -> new ExecutionException(Bundle.message("robot.run.no.interpreter")));
        final @NotNull Path output = Path.of(PathManager.getTempPath(), "testin-robot", UUID.randomUUID().toString());

        final @NotNull KillableColoredProcessHandler handler = new KillableColoredProcessHandler(configuration.commandLine(interpreter, output)) {
            // Rule-CODEGEN-110, Rule-CODEGEN-120
            @Override
            protected void onOSProcessTerminated(final int exitCode) {
                RobotOutput.report(project, output.resolve("output.xml"), configuration.getTestCaseUuids());
                super.onOSProcessTerminated(exitCode);
            }
        };
        ProcessTerminatedListener.attach(handler);

        return handler;
    }
}
