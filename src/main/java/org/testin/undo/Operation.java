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

package org.testin.undo;

import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public record Operation(@NotNull String description, @NotNull UndoStep undo, @NotNull UndoStep redo, @NotNull Runnable forget) {
    // UC-INTERNAL-005, Rule-INTERNAL-063
    public Operation(final @NotNull String description, final @NotNull BooleanSupplier undo, final @NotNull BooleanSupplier redo, final @NotNull Runnable forget) {
        this(description, atOnce(undo), atOnce(redo), forget);
    }

    // UC-INTERNAL-005, Rule-INTERNAL-063
    public Operation(final @NotNull String description, final @NotNull Runnable undo, final @NotNull Runnable redo) {
        this(description, always(undo), always(redo), () -> {
        });
    }

    private static @NotNull UndoStep atOnce(final @NotNull BooleanSupplier work) {
        return finished -> finished.accept(work.getAsBoolean());
    }

    private static @NotNull BooleanSupplier always(final @NotNull Runnable work) {
        return () -> {
            work.run();
            return true;
        };
    }
}
