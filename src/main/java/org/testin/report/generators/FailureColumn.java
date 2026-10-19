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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportEmphasis;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorFields;

import java.util.function.Function;

@Getter
@AllArgsConstructor
enum FailureColumn {
    PRIORITY(
            TestRunEditorFields.BUG_PRIORITY,
            runItem -> runItem.shownExecution().getBugPriority().getEmphasis()
    ),

    SEVERITY(
            TestRunEditorFields.BUG_SEVERITY,
            runItem -> runItem.shownExecution().getBugSeverity().getEmphasis()
    );

    private final @NotNull TestRunEditorFields field;

    private final @NotNull Function<RunItem, ReportEmphasis> emphasis;

    // Rule-REPORT-017, Rule-REPORT-019
    @NotNull String textOf(final @NotNull RunItem runItem) {
        return field.gridValue(runItem);
    }

    // Rule-REPORT-017
    @NotNull ReportEmphasis emphasisOf(final @NotNull RunItem runItem) {
        return emphasis.apply(runItem);
    }
}
