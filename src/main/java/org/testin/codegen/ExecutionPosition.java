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

package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.FromContentModule;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionPosition {
    // UC-CODEGEN-002, Rule-CODEGEN-014
    @FromContentModule
    public static int of(final @NotNull Project project, final @NotNull TestCase testCase) {
        return ofEach(project).applyAsInt(testCase);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014
    public static @NotNull ToIntFunction<TestCase> ofEach(final @NotNull Project project) {
        final @NotNull Map<Path, List<TestCase>> sets = new HashMap<>();

        return testCase -> TestCaseOrder.positionOf(sets.computeIfAbsent(testCase.getParent().getPath(), _ -> setOf(project, testCase)), testCase);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-014, Rule-VIEW-PANEL-062
    public static @NotNull Map<UUID, Integer> placesOf(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<Path, List<TestCase>> sets = new HashMap<>();
        final @NotNull Map<UUID, Integer> places = new HashMap<>();

        for (final TestCase testCase : testCases) {
            final @NotNull List<TestCase> set = sets.computeIfAbsent(testCase.getParent().getPath(), _ -> setOf(project, testCase));
            final int place = TestCaseOrder.positionOf(set, testCase);
            if (place <= set.size()) places.put(testCase.getUuid(), place);
        }

        return places;
    }

    // UC-CODEGEN-011, Rule-CODEGEN-042
    public static @NotNull List<TestCase> setOf(final @NotNull Project project, final @NotNull TestCase testCase) {
        return setAt(project, testCase.getParent().getPath());
    }

    // UC-CODEGEN-011, Rule-CODEGEN-042
    public static @NotNull List<TestCase> setAt(final @NotNull Project project, final @NotNull Path testSetPath) {
        return TestCaseOrder.ordered(Services.getInstance(project, TestCases.class).getTestCasesForTestSet(testSetPath));
    }
}
