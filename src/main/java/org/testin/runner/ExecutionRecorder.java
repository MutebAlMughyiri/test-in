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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestName;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
import org.testin.services.Services;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service(Service.Level.PROJECT)
public final class ExecutionRecorder implements Disposable {
    private final @NotNull Project project;

    private final @NotNull List<Reported> surfaces = new CopyOnWriteArrayList<>();

    ExecutionRecorder(final @NotNull Project project) {
        this.project = project;

        project.getMessageBus().connect(this).subscribe(ExecutionReported.TOPIC,
                (ExecutionReported) (testName, status, duration, failure) ->
                        ApplicationManager.getApplication().invokeLater(
                                () -> record(testName, status, duration, failure)));
    }

    public static void initRecording(final @NotNull Project project) {
        Services.getInstance(project, ExecutionRecorder.class);
    }

    public static void onReported(final @NotNull Project project, final @NotNull Disposable parentDisposable, final @NotNull Reported onUpdated) {
        final @NotNull ExecutionRecorder recorder = Services.getInstance(project, ExecutionRecorder.class);

        recorder.surfaces.add(onUpdated);
        Disposer.register(parentDisposable, () -> recorder.surfaces.remove(onUpdated));
    }

    private void record(final @NotNull String testName, final @NotNull ExecutionStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        TestinLogger.debug("Execution report: testName='" + testName + "', status='" + status + "'");

        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

        final @NotNull Attempt attempt = new Attempt(status, duration, failure, TestName.parameters(testName), ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS));

        TestName.testCaseId(testName).flatMap(testCases::findTestCase).ifPresentOrElse(
                testCase -> {
                    final @NotNull TestNGExecution execution = Services.getInstance(project, TestNGExecution.class);
                    execution.attempted(testCase.getId(), attempt).forEach((testCaseId, decided) -> testCases.findTestCase(testCaseId).ifPresent(decidedTestCase -> report(decidedTestCase, decided)));
                    execution.takeUnwritten(testCase.getId()).ifPresent(live -> show(testCase, live));
                },
                () -> TestinLogger.debug("  '" + testName + "' is not a generated test case - reported against none"));
    }

    // Rule-CODEGEN-111
    private void show(final @NotNull TestCaseDto testCase, final @NotNull Attempts live) {
        final @NotNull List<RunItemExecution> executions = live.executions();

        surfaces.forEach(surface -> surface.accept(testCase, ExecutionStatus.RUNNING, executions, live.written()));
    }

    // Rule-CODEGEN-110, Rule-CODEGEN-111
    private void report(final @NotNull TestCaseDto testCase, final @NotNull Attempts decided) {
        final @NotNull ExecutionStatus reportedStatus = runItemStatusFor(testCase, decided.status());
        final @NotNull List<RunItemExecution> executions = reportedStatus == ExecutionStatus.IDLE ? List.of() : decided.decidedExecutions();

        TestinLogger.debug("  reporting on '" + testCase.getDescription() + "': " + reportedStatus + " from " + decided.all().size() + " attempt(s)");

        Services.getInstance(project, TestNGExecution.class).reported(testCase.getId(), reportedStatus);

        surfaces.forEach(surface -> surface.accept(testCase, reportedStatus, executions, decided.written()));
    }

    @Override
    public void dispose() {
        surfaces.clear();
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    private @NotNull ExecutionStatus runItemStatusFor(final @NotNull TestCaseDto testCase, final @NotNull ExecutionStatus status) {
        final boolean stopped = status == ExecutionStatus.FAILED
                && Services.getInstance(project, TestNGExecution.class).isStopped(testCase);

        return stopped ? ExecutionStatus.IDLE : status;
    }

    @FunctionalInterface
    public interface Reported {
        void accept(final @NotNull TestCaseDto testCase, final @NotNull ExecutionStatus status, final @NotNull List<RunItemExecution> executions, final int written);
    }
}
