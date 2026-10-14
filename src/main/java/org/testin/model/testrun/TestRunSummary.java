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

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record TestRunSummary(long total, long passed, long failed, long blocked, long unjudged, long removed, @NotNull String executedBy) {
    public static final @NotNull TestRunSummary EMPTY = new TestRunSummary(0, 0, 0, 0, 0, 0, "");

    // UC-INTERNAL-006, Rule-INTERNAL-048, Rule-INTERNAL-049
    public static @NotNull TestRunSummary of(final @NotNull List<RunItem> runItems) {
        final @NotNull Map<RunItemStatus, Long> counts = runItems.stream()
                .collect(Collectors.groupingBy(RunItem::getStatus, Collectors.counting()));

        return new TestRunSummary(
                runItems.size(),
                counts.getOrDefault(RunItemStatus.PASSED, 0L),
                counts.getOrDefault(RunItemStatus.FAILED, 0L),
                counts.getOrDefault(RunItemStatus.BLOCKED, 0L),
                counts.getOrDefault(RunItemStatus.PENDING, 0L) + counts.getOrDefault(RunItemStatus.UNTESTED, 0L),
                counts.getOrDefault(RunItemStatus.REMOVED, 0L),
                whoExecuted(runItems));
    }

    private static @NotNull String whoExecuted(final @NotNull List<RunItem> runItems) {
        return runItems.stream()
                .map(RunItem::shownExecution).map(RunItemExecution::getExecutedBy)
                .filter(name -> !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public boolean hasRemoved() {
        return removed > 0;
    }

    public long executed() {
        return passed + failed + blocked;
    }

    // UC-INTERNAL-006, Rule-INTERNAL-049
    public int passRate() {
        return executed() > 0 ? Math.round((float) passed * 100 / executed()) : 0;
    }
}
