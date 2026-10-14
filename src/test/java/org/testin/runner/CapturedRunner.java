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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ExtensionTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.model.testrun.Failure;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.services.Services;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CapturedRunner implements AutomationExecutor {
    private final @NotNull List<List<TestCase>> executions = new CopyOnWriteArrayList<>();

    public static @NotNull CapturedRunner installed(final @NotNull Disposable owner) {
        final @NotNull CapturedRunner runner = new CapturedRunner();
        ExtensionTestUtil.maskExtensions(AutomationExecutor.EP, List.of(runner), owner);
        return runner;
    }

    public static void reports(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull TestNGExecutionStatus testNGExecutionStatus, final @NotNull Duration duration) {
        TestNGExecutionReported.broadcast(project, testCase.getId().toString(), testNGExecutionStatus, duration, Failure.NONE);
    }

    public static void startedByTestNg(final @NotNull Project project, final @NotNull TestCase testCase) {
        Services.getInstance(project, TestNGExecution.class).launched(List.of(testCase), "Testin: captured");
        reports(project, testCase, TestNGExecutionStatus.RUNNING, Duration.ZERO);
    }

    public @NotNull List<List<TestCase>> executions() {
        return List.copyOf(executions);
    }

    @Override
    public void run(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        executions.add(List.copyOf(testCases));
        testCases.forEach(Services.getInstance(project, TestNGExecution.class)::starting);
    }
}
