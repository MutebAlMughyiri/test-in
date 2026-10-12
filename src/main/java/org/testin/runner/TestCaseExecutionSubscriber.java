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
import org.testin.model.testrun.Execution;
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
public final class TestCaseExecutionSubscriber implements Disposable {
    private final @NotNull Project p;

    private final @NotNull List<Reported> surfaces = new CopyOnWriteArrayList<>();

    TestCaseExecutionSubscriber(final @NotNull Project p) {
        this.p = p;

        p.getMessageBus().connect(this).subscribe(TestCaseExecutionListener.TOPIC,
                (TestCaseExecutionListener) (testName, status, duration, failure) ->
                        ApplicationManager.getApplication().invokeLater(
                                () -> record(testName, status, duration, failure)));
    }

    public static void initRecording(final @NotNull Project p) {
        Services.getInstance(p, TestCaseExecutionSubscriber.class);
    }

    public static void onReported(final @NotNull Project p, final @NotNull Disposable parentDisposable, final @NotNull Reported onUpdated) {
        final @NotNull TestCaseExecutionSubscriber recorder = Services.getInstance(p, TestCaseExecutionSubscriber.class);

        recorder.surfaces.add(onUpdated);
        Disposer.register(parentDisposable, () -> recorder.surfaces.remove(onUpdated));
    }

    private void record(final @NotNull String testName, final @NotNull ExecutionStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        TestinLogger.debug("Execution report: testName='" + testName + "', status='" + status + "'");

        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);

        final @NotNull Attempt attempt = new Attempt(status, duration, failure, TestName.parameters(testName), ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS));

        TestName.testCaseId(testName).flatMap(testCases::findTestCase).ifPresentOrElse(
                tc -> {
                    final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);
                    execution.attempted(tc.getId(), attempt).forEach((id, decided) -> testCases.findTestCase(id).ifPresent(decidedTc -> report(decidedTc, decided)));
                    execution.takeUnwritten(tc.getId()).ifPresent(live -> show(tc, live));
                },
                () -> TestinLogger.debug("  '" + testName + "' is not a generated test case - reported against none"));
    }

    // Rule-CODEGEN-111
    private void show(final @NotNull TestCaseDto tc, final @NotNull Attempts live) {
        final @NotNull List<Execution> executions = live.executions();

        surfaces.forEach(surface -> surface.accept(tc, ExecutionStatus.RUNNING, executions, live.written()));
    }

    // Rule-CODEGEN-110, Rule-CODEGEN-111
    private void report(final @NotNull TestCaseDto tc, final @NotNull Attempts decided) {
        final @NotNull ExecutionStatus reportedStatus = runItemStatusFor(tc, decided.status());
        final @NotNull List<Execution> executions = reportedStatus == ExecutionStatus.IDLE ? List.of() : decided.decidedExecutions();

        TestinLogger.debug("  reporting on '" + tc.getDescription() + "': " + reportedStatus + " from " + decided.all().size() + " attempt(s)");

        Services.getInstance(p, TestNGExecution.class).reported(tc.getId(), reportedStatus);

        surfaces.forEach(surface -> surface.accept(tc, reportedStatus, executions, decided.written()));
    }

    @Override
    public void dispose() {
        surfaces.clear();
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    private @NotNull ExecutionStatus runItemStatusFor(final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status) {
        final boolean stopped = status == ExecutionStatus.FAILED
                && Services.getInstance(p, TestNGExecution.class).isStopped(tc);

        return stopped ? ExecutionStatus.IDLE : status;
    }

    @FunctionalInterface
    public interface Reported {
        void accept(final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status, final @NotNull List<Execution> executions, final int written);
    }
}
