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

package org.testin.codegen.writer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.NoCodeChange;
import org.testin.util.FromContentModule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record CodeChangeBinding<X>(@NotNull CodeChangeType<X> type, @NotNull CodeChange<X> action) {
    @FromContentModule
    public static <T> void bind(final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> bindings, final @NotNull CodeChangeType<T> type, final @NotNull CodeChange<T> action) {
        bindings.put(type, new CodeChangeBinding<>(type, action));
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005
    @FromContentModule
    public static <T> @NotNull CodeChange<T> actionFor(final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> bindings, final @NotNull CodeChangeType<T> type) {
        return Optional.ofNullable(bindings.get(type))
                .<CodeChange<T>>map(CodeChangeBinding::typed)
                .orElseGet(() -> new NoCodeChange<>(type.description()));
    }

    <T> @NotNull CodeChange<T> typed() {
        return new CodeChange<>() {
            @Override
            public void execute(final @NotNull Project project, final @NotNull T payload) {
                action.execute(project, type.payload().cast(payload));
            }

            @Override
            public void executeAll(final @NotNull Project project, final @NotNull List<? extends T> payloads) {
                action.executeAll(project, payloads.stream().map(type.payload()::cast).toList());
            }

            @Override
            public boolean generates() {
                return action.generates();
            }
        };
    }
}
