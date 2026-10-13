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

package org.testin.report;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.NameSanitizer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportFileName {
    private static final @NotNull DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd-MM-yyyy_hh-mm-ssa", Locale.US);

    // UC-REPORT-001, Rule-REPORT-006
    public static @NotNull String suggestedFor(final @NotNull Project project, final @NotNull TestRunNode testRun, final @NotNull ZonedDateTime at) {
        return of(Services.getInstance(project, BoundTestProject.class).name(), testRun.getName(), at);
    }

    // UC-REPORT-001, Rule-REPORT-007, Rule-REPORT-008
    static @NotNull String of(final @NotNull String testProjectName, final @NotNull String testRunName, final @NotNull ZonedDateTime at) {
        return Stream.of("TestRun", safe(testProjectName), safe(testRunName), STAMP.format(at))
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("_"));
    }

    private static @NotNull String safe(final @NotNull String name) {
        return NameSanitizer.removeSpecialChars(name).replace(" ", "").trim();
    }
}
