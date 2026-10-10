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

package org.testin.bug;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.RunItem;
import org.testin.report.generators.ReportText;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.util.Display;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record BugFacts(@NotNull String title, @NotNull BugSeverity severity, @NotNull BugPriority priority, @NotNull String platform, @NotNull String actualResult, @NotNull String expectedResult, @NotNull List<String> steps, @NotNull String testData, @NotNull String stacktrace, @NotNull List<byte[]> screenshots, @NotNull String testRun, @NotNull String executed, @NotNull String browser, @NotNull String device, @NotNull String language, @NotNull String commit, @NotNull UUID testCaseId, @NotNull String testSetName) {
    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-068
    public static @NotNull BugFacts of(final @NotNull RunItem runItem, final @NotNull TestCaseDto tc, final @NotNull TestRunMarker testRunMarker, final @NotNull String testRun, final @NotNull List<byte[]> screenshots) {
        return new BugFacts(
                TestSetEditorAttributes.DESCRIPTION.displayValue(tc),
                runItem.getBugSeverity(),
                runItem.getBugPriority(),
                ReportText.joined(BugTemplate.SEPARATOR, TestRunConfiguration.PLATFORM.valueIn(testRunMarker), TestRunConfiguration.COMPONENT.valueIn(testRunMarker)),
                runItem.getActualResult(),
                TestSetEditorAttributes.EXPECTED_RESULT.displayValue(tc),
                tc.getSteps().stream().map(Display::format).toList(),
                tc.getTestData(),
                runItem.getStacktrace(),
                screenshots,
                testRun,
                ReportText.joined(BugTemplate.SEPARATOR, runItem.getExecutedBy(), Display.formatDate(runItem.getExecutedAt())),
                TestRunConfiguration.BROWSER.valueIn(testRunMarker),
                TestRunConfiguration.DEVICE_TYPE.valueIn(testRunMarker),
                TestRunConfiguration.LANGUAGE.valueIn(testRunMarker),
                TestRunConfiguration.COMMIT.valueIn(testRunMarker),
                tc.getId(),
                tc.getParent().getName());
    }
}
