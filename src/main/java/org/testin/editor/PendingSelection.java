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

import java.util.Optional;
import java.util.UUID;

public final class PendingSelection {
    private @NotNull Optional<UUID> waiting = Optional.empty();
    private boolean focus;

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-104
    public void waitFor(final @NotNull UUID testCaseUuid) {
        waiting = Optional.of(testCaseUuid);
        focus = true;
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-118
    public void keep(final @NotNull Optional<UUID> testCaseUuid) {
        waiting = testCaseUuid;
    }

    public @NotNull Optional<UUID> waiting() {
        return waiting;
    }

    public @NotNull Optional<UUID> take() {
        final @NotNull Optional<UUID> taken = waiting;
        waiting = Optional.empty();
        return taken;
    }

    public void forget() {
        waiting = Optional.empty();
    }

    public boolean takeFocus() {
        final boolean wanted = focus;
        focus = false;
        return wanted;
    }
}
