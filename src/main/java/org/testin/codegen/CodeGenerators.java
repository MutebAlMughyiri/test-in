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

package org.testin.codegen;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

public interface CodeGenerators {
    @NotNull ExtensionPointName<CodeGenerators> EP = ExtensionPointName.create("org.testin.codeGenerators");

    // UC-CODEGEN-019, Rule-CODEGEN-005
    static <T> @NotNull GenAction<T> find(final @NotNull GenType<T> type) {
        return EP.getExtensionList().stream()
                .findFirst()
                .map(generators -> generators.actionFor(type))
                .orElseGet(() -> new NoJavaCode<>(type.description()));
    }

    <T> @NotNull GenAction<T> actionFor(final @NotNull GenType<T> type);
}
