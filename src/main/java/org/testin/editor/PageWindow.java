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

package org.testin.editor;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import java.util.List;
import java.util.UUID;

public record PageWindow(int page, int totalPages, int fromIndex, int toIndex) {
    public static @NotNull PageWindow of(final int testCaseCount, final int requestedPage, final int pageSize) {
        final int safePageSize = Math.max(1, pageSize);
        final int safeTestCaseCount = Math.max(0, testCaseCount);
        final int totalPages = Math.max(1, (safeTestCaseCount + safePageSize - 1) / safePageSize);
        final int page = Math.clamp(requestedPage, 1, totalPages);
        final int fromIndex = Math.min((page - 1) * safePageSize, safeTestCaseCount);
        final int toIndex = Math.min(fromIndex + safePageSize, safeTestCaseCount);
        return new PageWindow(page, totalPages, fromIndex, toIndex);
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-104
    public static int pageContaining(final @NotNull UUID testCaseUuid, final @NotNull List<TestCase> testCases, final int pageSize) {
        final int safePageSize = Math.max(1, pageSize);
        for (int i = 0; i < testCases.size(); i++) {
            if (testCaseUuid.equals(testCases.get(i).getUuid())) return (i / safePageSize) + 1;
        }

        return 0;
    }

    public boolean isEmpty() {
        return fromIndex == toIndex;
    }
}
