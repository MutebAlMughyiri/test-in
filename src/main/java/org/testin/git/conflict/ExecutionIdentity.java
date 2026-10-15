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

package org.testin.git.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

// Rule-SHARE-133
record ExecutionIdentity(@NotNull String status, @NotNull Instant executedAt, int invocation, int retry) {
    private static final @NotNull String INVOCATION = "invocation";

    private static final @NotNull String RETRY = "retry";

    static @NotNull ExecutionIdentity of(final @NotNull JsonNode execution) {
        return new ExecutionIdentity(execution.path(RunItemMerger.STATUS).asText(""), RunItemMerger.executedAt(execution).toInstant(), execution.path(INVOCATION).asInt(), execution.path(RETRY).asInt());
    }
}
