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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter2.Filters;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestName;
import org.testin.logger.TestinLogger;
import org.testin.model.status.AutomationExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testin.runner.AutomationExecutionReported;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RobotOutput {
    private static final @NotNull Map<String, AutomationExecutionStatus> STATUSES = Map.of(
            "PASS", AutomationExecutionStatus.PASSED,
            "FAIL", AutomationExecutionStatus.FAILED);

    // UC-CODEGEN-008, Rule-CODEGEN-120
    static void report(final @NotNull Project project, final @NotNull Path output, final @NotNull Collection<UUID> wanted) {
        try {
            final @NotNull List<RobotTestExecution> executions = executionsIn(JDOMUtil.load(output), wanted);
            ApplicationManager.getApplication().invokeLater(() -> executions.forEach(execution ->
                    AutomationExecutionReported.broadcast(project, execution.testCaseUuid().toString(), execution.automationExecutionStatus(), execution.duration(), execution.failure())));
        } catch (final IOException | JDOMException ex) {
            TestinLogger.warn("Robot Framework left no output to read at " + output + ": " + FailureText.of(ex));
        }
    }

    // UC-CODEGEN-008, Rule-CODEGEN-075, Rule-CODEGEN-120
    static @NotNull List<RobotTestExecution> executionsIn(final @NotNull Element robot, final @NotNull Collection<UUID> wanted) {
        final @NotNull List<RobotTestExecution> executions = new ArrayList<>();
        final @NotNull Iterator<Element> tests = robot.getDescendants(Filters.element("test"));

        while (tests.hasNext()) {
            final @NotNull Element test = tests.next();
            final @NotNull Optional<UUID> testCaseUuid = test.getChildren("tag").stream()
                    .map(tag -> TestName.testCaseUuid(tag.getTextTrim()))
                    .flatMap(Optional::stream)
                    .filter(wanted::contains)
                    .findFirst();
            testCaseUuid.ifPresent(found -> Optional.ofNullable(test.getChild("status")).ifPresent(status -> executions.add(executionOf(found, status))));
        }

        return executions;
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-120
    private static @NotNull RobotTestExecution executionOf(final @NotNull UUID testCaseUuid, final @NotNull Element status) {
        final @NotNull AutomationExecutionStatus automationExecutionStatus = STATUSES.getOrDefault(status.getAttributeValue("status", ""), AutomationExecutionStatus.SKIPPED);
        final @NotNull String message = status.getTextTrim();
        final @NotNull Failure failure = automationExecutionStatus == AutomationExecutionStatus.PASSED
                ? Failure.NONE
                : new Failure(message.isEmpty() ? Bundle.message("runner.skipped") : message, "", List.of());

        return new RobotTestExecution(testCaseUuid, automationExecutionStatus, durationOf(status.getAttributeValue("elapsed", "")), failure);
    }

    private static @NotNull Duration durationOf(final @NotNull String seconds) {
        try {
            return Duration.ofMillis(Math.round(Double.parseDouble(seconds) * 1000));
        } catch (final NumberFormatException ex) {
            return Duration.ZERO;
        }
    }
}
