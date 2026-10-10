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
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Bundle;

import java.time.Duration;

// UC-CODEGEN-021, Rule-CODEGEN-083
public record AgentConnection(@NotNull String command, @NotNull String arguments, @NotNull String prompt, @NotNull Duration timeout) {
    // UC-CODEGEN-021, Rule-CODEGEN-083
    public static @NotNull AgentConnection stored() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        return new AgentConnection(settings.agentCommand, settings.agentArguments, settings.agentPrompt,
                Duration.ofSeconds(settings.agentTimeoutSeconds));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    public boolean isConnected() {
        return !command.isBlank();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    public @NotNull String promptTemplate() {
        return prompt.isBlank() ? Bundle.message("agent.prompt.default") : prompt;
    }
}
