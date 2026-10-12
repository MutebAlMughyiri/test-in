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
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
import org.testin.services.Services;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CapturedRunner implements TestRunner {
    private final @NotNull List<List<TestCaseDto>> runs = new CopyOnWriteArrayList<>();

    public static @NotNull CapturedRunner installed(final @NotNull Disposable owner) {
        final @NotNull CapturedRunner runner = new CapturedRunner();
        ExtensionTestUtil.maskExtensions(TestRunner.EP, List.of(runner), owner);
        return runner;
    }

    public static void reports(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status, final @NotNull Duration duration) {
        TestCaseExecutionListener.broadcast(p, tc.getId().toString(), status, duration, Failure.NONE);
    }

    public static void startedByTestNg(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        Services.getInstance(p, TestNGExecution.class).launched(List.of(tc), "Testin: captured");
        reports(p, tc, ExecutionStatus.RUNNING, Duration.ZERO);
    }

    public @NotNull List<List<TestCaseDto>> runs() {
        return List.copyOf(runs);
    }

    @Override
    public void run(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        runs.add(List.copyOf(testCases));
        testCases.forEach(Services.getInstance(p, TestNGExecution.class)::starting);
    }
}
