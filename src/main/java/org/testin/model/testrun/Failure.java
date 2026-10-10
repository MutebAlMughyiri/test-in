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

package org.testin.model.testrun;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Failure(@NotNull String message, @NotNull String stacktrace) {
    public static final @NotNull Failure NONE = new Failure("", "");

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220
    public void recordOn(final @NotNull RunItem runItem) {
        if (isNothing()) return;

        FailureDetail.clear(runItem, FailureDetail.WHAT_HAPPENED);
        runItem.setActualResult(message);
        runItem.setStacktrace(stacktrace);
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220
    public @NotNull List<String> wouldClear(final @NotNull RunItem runItem) {
        return isNothing() ? List.of() : FailureDetail.filledIn(runItem, FailureDetail.WHAT_HAPPENED);
    }

    private boolean isNothing() {
        return message.isBlank() && stacktrace.isBlank();
    }
}
