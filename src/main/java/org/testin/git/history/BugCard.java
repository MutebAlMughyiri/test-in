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
import org.testin.model.DateFormats;

import java.time.ZonedDateTime;

public record BugCard(@NotNull String hash, @NotNull String who, @NotNull ZonedDateTime when, @NotNull BugEvent event) implements HistoryCard {
    // Rule-VIEW-PANEL-115
    @Override
    public @NotNull BugEventKind kind() {
        return event.kind();
    }

    // Rule-VIEW-PANEL-107
    static @NotNull BugCard notCommitted(final @NotNull BugEvent event) {
        if (event.kind() == BugEventKind.REMOVED) return new BugCard("", "", DateFormats.NOT_EXECUTED, event);

        return new BugCard("", event.runItem().shownExecution().getExecutedBy(), event.runItem().shownExecution().getExecutedAt(), event);
    }
}
