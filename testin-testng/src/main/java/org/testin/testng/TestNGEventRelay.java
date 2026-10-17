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

package org.testin.testng;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.stacktrace.DiffHyperlink;
import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testin.runner.TestNGExecutionReported;
import org.testin.util.Bundle;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

// UC-CODEGEN-008, Rule-CODEGEN-075, Rule-CODEGEN-110
@RequiredArgsConstructor
public final class TestNGEventRelay extends SMTRunnerEventsAdapter {
    private final @NotNull Project project;

    // Rule-CODEGEN-075, Rule-CODEGEN-110
    static @NotNull FinishedAttempt finished(final @NotNull SMTestProxy test) {
        if (test.isPassed()) return new FinishedAttempt(TestNGExecutionStatus.PASSED, Failure.NONE);
        if (test.isDefect() && !test.isIgnored() && !test.wasTerminated())
            return new FinishedAttempt(TestNGExecutionStatus.FAILED, failureOf(test, ""));
        if (test.isIgnored() && !test.wasTerminated())
            return new FinishedAttempt(TestNGExecutionStatus.SKIPPED, failureOf(test, Bundle.message("runner.skipped")));
        return new FinishedAttempt(TestNGExecutionStatus.FAILED, failureOf(test, Bundle.message("runner.skipped")));
    }

    private static @NotNull Duration durationOf(final @NotNull SMTestProxy test) {
        final Long millis = test.getDuration();

        return millis == null ? Duration.ZERO : Duration.ofMillis(millis);
    }

    private static @NotNull Failure failureOf(final @NotNull SMTestProxy test, final @NotNull String fallback) {
        return new Failure(messageOf(test, fallback), Objects.toString(test.getStacktrace(), "").strip(), List.of());
    }

    private static @NotNull String messageOf(final @NotNull SMTestProxy test, final @NotNull String fallback) {
        final @NotNull String message = Objects.toString(test.getErrorMessage(), fallback).strip();

        final DiffHyperlink comparison = test.getDiffViewerProvider();
        if (comparison == null) return message;

        return Bundle.message("runner.comparison", message, comparison.getLeft(), comparison.getRight());
    }

    // UC-CODEGEN-008
    @Override
    public void onTestStarted(final @NotNull SMTestProxy test) {
        TestNGExecutionReported.broadcast(project, test.getPresentableName(), TestNGExecutionStatus.RUNNING, Duration.ZERO, Failure.NONE);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-075, Rule-CODEGEN-110
    @Override
    public void onTestFinished(final @NotNull SMTestProxy test) {
        final @NotNull FinishedAttempt finished = finished(test);
        TestNGExecutionReported.broadcast(project, test.getPresentableName(), finished.testNGExecutionStatus(), durationOf(test), finished.failure());
    }
}
