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

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.stacktrace.DiffHyperlink;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
import org.testin.services.ProjectLifetime;
import org.testin.util.Bundle;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseExecutionTracker {
    public static void initGlobalListener(final @NotNull Project p) {
        p.getMessageBus().connect(ProjectLifetime.of(p)).subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestStarted(final @NotNull SMTestProxy test) {
                TestCaseExecutionListener.broadcast(p, test.getPresentableName().toLowerCase(Locale.ROOT), ExecutionStatus.RUNNING, Duration.ZERO, Failure.NONE);
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                finished(p, test);
            }
        });
    }

    // UC-CODEGEN-008, Rule-CODEGEN-075, Rule-CODEGEN-110
    static void finished(final @NotNull Project p, final @NotNull SMTestProxy test) {
        final @NotNull String testName = test.getPresentableName().toLowerCase(Locale.ROOT);

        if (test.isPassed()) {
            TestCaseExecutionListener.broadcast(p, testName, ExecutionStatus.PASSED, durationOf(test), Failure.NONE);

        } else if (test.isDefect() && !test.isIgnored() && !test.wasTerminated()) {
            TestCaseExecutionListener.broadcast(p, testName, ExecutionStatus.FAILED, durationOf(test), failureOf(test, ""));

        } else if (test.isIgnored() && !test.wasTerminated()) {
            TestCaseExecutionListener.broadcast(p, testName, ExecutionStatus.SKIPPED, durationOf(test), failureOf(test, Bundle.message("runner.skipped")));

        } else {
            TestCaseExecutionListener.broadcast(p, testName, ExecutionStatus.FAILED, durationOf(test), failureOf(test, Bundle.message("runner.skipped")));
        }
    }

    private static @NotNull Duration durationOf(final @NotNull SMTestProxy test) {
        final Long millis = test.getDuration();

        return millis == null ? Duration.ZERO : Duration.ofMillis(millis);
    }

    private static @NotNull Failure failureOf(final @NotNull SMTestProxy test, final @NotNull String fallback) {
        return new Failure(messageOf(test, fallback), Objects.toString(test.getStacktrace(), "").strip());
    }

    private static @NotNull String messageOf(final @NotNull SMTestProxy test, final @NotNull String fallback) {
        final @NotNull String message = Objects.toString(test.getErrorMessage(), fallback).strip();

        final DiffHyperlink comparison = test.getDiffViewerProvider();
        if (comparison == null) return message;

        return Bundle.message("runner.comparison", message, comparison.getLeft(), comparison.getRight());
    }
}
