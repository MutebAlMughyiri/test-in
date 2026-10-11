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
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
import org.testin.services.Services;

import java.time.Duration;
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
        Logger.debug("Execution report: testName='" + testName + "', status='" + status + "'");

        TestName.testCaseId(testName).flatMap(Services.getInstance(p, TestCases.class)::findTestCase).ifPresentOrElse(
                tc -> report(tc, status, duration, failure),
                () -> Logger.debug("  '" + testName + "' is not a generated test case - reported against none"));
    }

    private void report(final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        final @NotNull ExecutionStatus reportedStatus = runItemStatusFor(tc, status);

        Logger.debug("  reporting on '" + tc.getDescription() + "': " + reportedStatus + " " + failure.message());

        Services.getInstance(p, TestNGExecution.class).reported(tc.getId(), reportedStatus, failure);

        surfaces.forEach(surface -> surface.accept(tc, reportedStatus, duration, failure));
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
        void accept(final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status, final @NotNull Duration duration, final @NotNull Failure failure);
    }
}
