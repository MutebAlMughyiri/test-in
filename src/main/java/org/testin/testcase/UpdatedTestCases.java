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

package org.testin.testcase;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdatedTestCases {
    // UC-EDITOR-PANEL-006, UC-VIEW-PANEL-011, Rule-EDITOR-PANEL-038, Rule-EDITOR-PANEL-039, Rule-EDITOR-PANEL-223
    public static int write(final @NotNull Project project, final @NotNull Path testSetPath, final @NotNull List<UUID> selectedUuids, final @NotNull TestCaseSnapshot before, final @NotNull List<TestCase> updatedTestCases) {
        final @NotNull List<TestCase> selected = updatedTestCases.stream().filter(testCase -> selectedUuids.contains(testCase.getUuid())).toList();
        final @NotNull List<UUID> alongside = TestCaseSnapshot.uuidsOf(updatedTestCases).stream().filter(testCaseUuid -> !selectedUuids.contains(testCaseUuid)).toList();
        final @NotNull TestCaseSnapshot everyBefore = before.with(TestCaseSnapshot.of(project, testSetPath, alongside));
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

        int written = 0;
        for (final TestCase testCase : updatedTestCases)
            if (testCases.putTestCase(testSetPath, testCase) && selected.contains(testCase)) written++;

        if (written > 0)
            TestCaseSnapshot.record(project, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), selected), everyBefore, TestCaseSnapshot.of(project, testSetPath, everyBefore.uuids()));

        return written;
    }
}
