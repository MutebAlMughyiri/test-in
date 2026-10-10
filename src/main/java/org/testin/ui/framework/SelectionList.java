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
import org.testin.util.Icons;

import javax.swing.Icon;

public record SelectionList<T>(@NotNull Icon icon, @NotNull String name, @NotNull String hint, @NotNull T value) {
    // UC-INTERNAL-007, Rule-INTERNAL-077
    public static <T> @NotNull SelectionList<T> add(final @NotNull Icon icon, final @NotNull String name, final @NotNull String hint, final @NotNull T value) {
        return new SelectionList<>(Icons.gray(icon), name, hint, value);
    }
}
