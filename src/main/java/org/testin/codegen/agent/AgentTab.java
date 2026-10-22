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

package org.testin.codegen.agent;

import org.jetbrains.annotations.NotNull;

import java.util.List;

// UC-CODEGEN-021, Rule-CODEGEN-129
public interface AgentTab {
    // Rule-CODEGEN-084, Rule-CODEGEN-129
    boolean typed(@NotNull String name, @NotNull String request);

    // Rule-CODEGEN-083, Rule-CODEGEN-129
    void opened(@NotNull String name, @NotNull List<String> commandLine, @NotNull String request);
}
