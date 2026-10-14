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

import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public record RunItemPath(@NotNull Path testRunPath, @NotNull UUID testCaseUuid) {
    public @NotNull Optional<RunItem> in(final @NotNull RunItems runItems) {
        return runItems.runItemOf(testCaseUuid).filter(runItem -> !runItem.isRemoved());
    }

    public @NotNull Optional<RunItem> failedIn(final @NotNull RunItems runItems) {
        return in(runItems).filter(runItem -> runItem.getStatus() == RunItemStatus.FAILED);
    }

    public @NotNull Optional<RunItem> stillFailed(final @NotNull TestRuns testRuns) {
        return testRuns.findRunItems(testRunPath).flatMap(this::failedIn);
    }
}
