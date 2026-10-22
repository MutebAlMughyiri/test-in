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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;
import java.util.stream.Stream;

// UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-089
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgentRequest {
    private static final @NotNull String NEXT_LINE = "\n";

    private static final @NotNull String SEND = "\r";

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-089, Rule-CODEGEN-090, Rule-CODEGEN-129
    public static void send(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull String request) {
        final @NotNull String typed = typedAs(request);
        TestinLogger.debug("The agent's request:\n" + request);

        final @NotNull AgentTab tab = Services.getInstance(project, AgentTab.class);
        if (!tab.typed(connection.name(), typed)) tab.opened(connection.name(), commandLine(connection), typed);

        Services.getInstance(project, Notifier.class).softShow(project, Done.SENT);
    }

    // Rule-CODEGEN-084
    static @NotNull String typedAs(final @NotNull String request) {
        return request.strip().replace("\r\n", NEXT_LINE).replace("\r", NEXT_LINE) + SEND;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    static @NotNull List<String> commandLine(final @NotNull AgentConnection connection) {
        return Stream.concat(Stream.of(AgentCli.executable(connection.command())), AgentCli.arguments(connection.arguments()).stream()).toList();
    }
}
