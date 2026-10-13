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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.TestRunSummary;
import org.testin.util.Bundle;

public record NodeFigures(long testSets, long packages, long testCases, long runnableTestCases, long testRuns, @NotNull TestRunSummary testRun, @NotNull TestRunStatus testRunStatus) {
    public static final @NotNull NodeFigures NONE = new NodeFigures(0, 0, 0, 0, 0, TestRunSummary.EMPTY, TestRunStatus.CREATED);

    public static @NotNull NodeFigures ofChildren(final long testSets, final long packages, final long testCases, final long runnableTestCases, final long testRuns) {
        return new NodeFigures(testSets, packages, testCases, runnableTestCases, testRuns, TestRunSummary.EMPTY, TestRunStatus.CREATED);
    }

    public static @NotNull NodeFigures ofTestRun(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus testRunStatus) {
        return new NodeFigures(0, 0, 0, 0, 0, summary, testRunStatus);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-052
    public @NotNull String rateLabel() {
        return testRun.executed() == 0 ? Bundle.message("figures.not.run") : testRun.passRate() + "%";
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
    public @NotNull String describe() {
        if (testSets == 0 && testCases == 0 && testRuns == 0) return "";

        final @NotNull String setsText = testSets == 1
                ? Bundle.message("figures.test.sets.one")
                : Bundle.message("figures.test.sets.many", String.valueOf(testSets));
        final @NotNull String testCasesText = testCases == 1
                ? Bundle.message("figures.test.cases.one")
                : Bundle.message("figures.test.cases.many", String.valueOf(testCases));
        final @NotNull String testRunsText = testRuns == 1
                ? Bundle.message("figures.test.runs.one")
                : Bundle.message("figures.test.runs.many", String.valueOf(testRuns));

        return Bundle.message("figures.holds", setsText, testCasesText, testRunsText);
    }
}
