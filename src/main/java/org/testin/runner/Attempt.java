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

package org.testin.runner;

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.testrun.Failure;

import java.time.Duration;
import java.util.List;

record Attempt(@NotNull ExecutionStatus status, @NotNull Duration duration, @NotNull Failure failure) {
    private static final @NotNull List<ExecutionStatus> FROM_BEST_TO_WORST = List.of(ExecutionStatus.SKIPPED, ExecutionStatus.PASSED, ExecutionStatus.FAILED);

    // Rule-CODEGEN-110
    @NotNull Attempt then(final @NotNull Attempt later) {
        return FROM_BEST_TO_WORST.indexOf(later.status) >= FROM_BEST_TO_WORST.indexOf(status) ? later : this;
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-110
    @NotNull Attempt decided() {
        return status == ExecutionStatus.SKIPPED ? new Attempt(ExecutionStatus.FAILED, duration, failure) : this;
    }
}
