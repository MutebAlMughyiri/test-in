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


package org.testin.help;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public record SetupHint(@NotNull SetupStep step, @NotNull String text, @NotNull Optional<Supplier<JComponent>> form, @NotNull String subject) {
    // UC-INTERNAL-009, Rule-INTERNAL-127
    public static @NotNull SetupHint of(final @NotNull SetupStep step, final @NotNull String text) {
        return new SetupHint(step, text, Optional.empty(), "");
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public static @NotNull SetupHint of(final @NotNull SetupStep step, final @NotNull String text, final @NotNull Supplier<JComponent> form) {
        return new SetupHint(step, text, Optional.of(form), "");
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public @NotNull SetupHint about(final @NotNull Path subject) {
        return new SetupHint(step, text, form, subject.toString());
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public static @NotNull SetupHint of(final @NotNull SetupStep step, final @NotNull String text, final @NotNull String link, final @NotNull Runnable fix) {
        return of(step, text, () -> PopupLink.of(link, fix));
    }
}
