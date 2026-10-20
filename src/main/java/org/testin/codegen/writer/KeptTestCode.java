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

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.util.FromContentModule;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
public final class KeptTestCode {
    private final @NotNull Map<UUID, String> byTestCaseUuid = new ConcurrentHashMap<>();

    // UC-CODEGEN-014, Rule-CODEGEN-114
    @FromContentModule
    public void keep(final @NotNull TestCase testCase, final @NotNull String code) {
        byTestCaseUuid.put(testCase.getUuid(), code);
    }

    // UC-CODEGEN-014, Rule-CODEGEN-114
    @FromContentModule
    public @NotNull Optional<String> take(final @NotNull TestCase testCase) {
        return Optional.ofNullable(byTestCaseUuid.remove(testCase.getUuid()));
    }
}
