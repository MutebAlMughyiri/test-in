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

package org.testin.git.history;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

record RecordedRevision(@NotNull String head, @NotNull String commit, @NotNull List<UUID> removedTestCaseUuids, @NotNull String revision) {
    // Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    boolean answers(final @NotNull String headNow, final @NotNull String commitNow, final @NotNull List<UUID> removedTestCaseUuidsNow) {
        return head.equals(headNow) && commit.equals(commitNow) && removedTestCaseUuids.equals(removedTestCaseUuidsNow);
    }
}
