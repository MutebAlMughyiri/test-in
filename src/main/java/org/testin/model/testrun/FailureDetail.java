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

package org.testin.model.testrun;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public enum FailureDetail {
    ACTUAL_RESULT(
            Bundle.message("failure.detail.actual.result"),
            runItem -> !runItem.getActualResult().isBlank(),
            runItem -> runItem.setActualResult("")
    ),

    STACKTRACE(
            Bundle.message("failure.detail.stacktrace"),
            runItem -> !runItem.getStacktrace().isBlank(),
            runItem -> runItem.setStacktrace("")
    ),

    SCREENSHOTS(
            Bundle.message("failure.detail.screenshots"),
            runItem -> !runItem.getScreenshots().isEmpty(),
            runItem -> runItem.setScreenshots(List.of())
    ),

    BUG_SEVERITY(
            Bundle.message("failure.detail.bug.severity"),
            runItem -> runItem.getBugSeverity() != BugSeverity.DEFAULT,
            runItem -> runItem.setBugSeverity(BugSeverity.DEFAULT)
    ),

    BUG_PRIORITY(
            Bundle.message("failure.detail.bug.priority"),
            runItem -> runItem.getBugPriority() != BugPriority.DEFAULT,
            runItem -> runItem.setBugPriority(BugPriority.DEFAULT)
    ),

    BUG_ISSUE_URL(
            Bundle.message("failure.detail.bug.issue.link"),
            runItem -> runItem.bugIssue().isPresent(),
            runItem -> runItem.setBugIssueUrl("")
    );

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220
    public static final @NotNull List<FailureDetail> WHAT_HAPPENED = List.of(ACTUAL_RESULT, STACKTRACE, SCREENSHOTS);
    private final @NotNull String label;
    private final @NotNull Predicate<RunItem> filled;
    private final @NotNull Consumer<RunItem> clear;

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-105
    public static boolean recordsABug(final @NotNull RunItem runItem) {
        return runItem.isFailed() || BUG_ISSUE_URL.filled.test(runItem);
    }

    public static @NotNull List<String> filledIn(final @NotNull RunItem runItem) {
        return filledIn(runItem, List.of(values()));
    }

    public static @NotNull List<String> filledIn(final @NotNull RunItem runItem, final @NotNull List<FailureDetail> details) {
        return details.stream()
                .filter(detail -> detail.filled.test(runItem))
                .map(FailureDetail::getLabel)
                .toList();
    }

    public static void clearAll(final @NotNull RunItem runItem) {
        clear(runItem, List.of(values()));
    }

    public static void clear(final @NotNull RunItem runItem, final @NotNull List<FailureDetail> details) {
        details.forEach(detail -> detail.clear.accept(runItem));
    }
}
