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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunSummary;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportText {
    public static final @NotNull String PLUGIN_NAME = "Testin";

    public static final @NotNull String PLUGIN_URL = "https://plugins.jetbrains.com/plugin/31514-testin";

    // Rule-REPORT-019
    public static final @NotNull String EMPTY = "—";

    // Rule-REPORT-019
    public static @NotNull String orEmpty(final @NotNull String value) {
        return value.isEmpty() ? EMPTY : value;
    }

    // Rule-REPORT-019
    public static @NotNull String subtitle(final @NotNull String testProjectName, final @NotNull TestRunMarker marker) {
        return joined("  |  ", testProjectName, joined(", ", TestRunConfiguration.PLATFORM.valueIn(marker), TestRunConfiguration.COMPONENT.valueIn(marker)));
    }

    // Rule-REPORT-019
    public static @NotNull String summary(final @NotNull String testRunName, final @NotNull TestRunSummary summary, final @NotNull UnaryOperator<String> emphasized) {
        return Bundle.message("report.summary.named", emphasized.apply(testRunName), emphasized.apply(String.valueOf(summary.total())),
                emphasized.apply(String.valueOf(summary.executed())), emphasized.apply(summary.passRate() + "%"));
    }

    // Rule-REPORT-019, Rule-REPORT-020
    static @NotNull List<NumberedSection> numberedSections(final @NotNull TestRunSummary summary, final boolean analyzed) {
        final @NotNull List<NumberedSection> numbered = new ArrayList<>();
        int number = analyzed ? 4 : 3;
        for (final ReportSection section : ReportSection.values()) {
            final long count = section.count(summary);
            if (count > 0) numbered.add(new NumberedSection(number++, section, count));
        }
        return numbered;
    }

    public static @NotNull String footerLead() {
        return Bundle.message("report.footer.prefix") + " ";
    }

    // UC-REPORT-001, Rule-REPORT-027
    public static @NotNull Optional<String> otherExecutions(final @NotNull RunItem runItem) {
        final @NotNull String others = runItem.otherFailuresAndBlocks().entrySet().stream()
                .map(other -> other.getValue() == 1
                        ? Bundle.message("report.other.execution", other.getKey().getLabel())
                        : Bundle.message("report.other.executions", other.getKey().getLabel(), String.valueOf(other.getValue())))
                .collect(Collectors.joining(", "));
        return others.isEmpty() ? Optional.empty() : Optional.of(others);
    }

    public static @NotNull String joined(final @NotNull String separator, final String @NotNull ... parts) {
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(separator));
    }
}
