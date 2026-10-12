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

package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

public record Alternative(@NotNull Shortcuts key, @NotNull String name, @NotNull Runnable action) {
    // Rule-INTERNAL-140
    public Alternative(final @NotNull String name, final @NotNull Runnable action) {
        this(Shortcuts.EMPTY, name, action);
    }
}
