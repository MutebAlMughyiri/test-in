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

package org.testin.report.generators;

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class ReportWarningTest {

    private static @NotNull RunItem executed(final @NotNull RunItemStatus... runItemExecutionStatuses) {
        return RunItem.builder().id(UUID.randomUUID())
                .executions(Arrays.stream(runItemExecutionStatuses).map(runItemExecutionStatus -> RunItemExecution.builder().status(runItemExecutionStatus).build()).toList())
                .build();
    }

    // Rule-REPORT-027
    @Test
    public void aRunItemExecutedOnceSaysNothingMore() {
        assertEquals(ReportText.otherExecutions(executed(RunItemStatus.FAILED)), Optional.empty());
    }

    // Rule-REPORT-027
    @Test
    public void aPassThatFailedBeforeSaysSo() {
        assertEquals(ReportText.otherExecutions(executed(RunItemStatus.FAILED, RunItemStatus.PASSED)),
                Optional.of(Bundle.message("report.other.execution", RunItemStatus.FAILED.getLabel())));
    }

    // Rule-REPORT-027
    @Test
    public void severalOthersAreCountedByRunItemStatusInOneLine() {
        assertEquals(ReportText.otherExecutions(executed(RunItemStatus.BLOCKED, RunItemStatus.FAILED, RunItemStatus.FAILED, RunItemStatus.PASSED)),
                Optional.of(Bundle.message("report.other.executions", RunItemStatus.FAILED.getLabel(), "2") + ", "
                        + Bundle.message("report.other.execution", RunItemStatus.BLOCKED.getLabel())));
    }

    // Rule-REPORT-027
    @Test
    public void theDefaultsOwnFailureAndOtherPassesAreNotWarnedAbout() {
        assertEquals(ReportText.otherExecutions(executed(RunItemStatus.PASSED, RunItemStatus.PASSED, RunItemStatus.FAILED)), Optional.empty(),
                "the run item is already Failed, and the passes are not a warning");
    }
}
