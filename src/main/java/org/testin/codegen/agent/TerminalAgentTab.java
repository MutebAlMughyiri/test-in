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
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.jediterm.terminal.TtyConnector;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.testin.logger.TestinLogger;
import org.testin.util.FailureText;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// UC-CODEGEN-021, Rule-CODEGEN-129
@RequiredArgsConstructor
public final class TerminalAgentTab implements AgentTab {
    private final @NotNull Project project;

    // Rule-CODEGEN-084, Rule-CODEGEN-129
    @Override
    public boolean typed(final @NotNull String name, final @NotNull String request) {
        final @NotNull Optional<ToolWindow> window = Optional.ofNullable(TerminalToolWindowManager.getInstance(project).getToolWindow());
        final @NotNull Optional<Content> running = tabsNamed(window, name)
                .filter(content -> connectorOf(content).filter(TtyConnector::isConnected).isPresent())
                .findFirst();
        if (running.isEmpty()) return false;

        window.ifPresent(terminal -> {
            terminal.getContentManager().setSelectedContent(running.orElseThrow());
            terminal.show();
        });
        connectorOf(running.orElseThrow()).ifPresent(agent -> typedInto(agent, request));
        return true;
    }

    // Rule-CODEGEN-083, Rule-CODEGEN-129
    @Override
    public void opened(final @NotNull String name, final @NotNull List<String> commandLine, final @NotNull String request) {
        final @NotNull TerminalToolWindowManager terminals = TerminalToolWindowManager.getInstance(project);
        final @NotNull TerminalTabState tab = new TerminalTabState();
        tab.myTabName = name;
        tab.myShellCommand = commandLine;
        tab.myWorkingDirectory = project.getBasePath();

        TestinLogger.debug("Starting the agent in the Terminal: " + String.join(" ", commandLine));
        terminals.createNewSession(LocalTerminalDirectRunner.createTerminalRunner(project), tab);

        tabsNamed(Optional.ofNullable(terminals.getToolWindow()), name).reduce((_, newest) -> newest)
                .flatMap(content -> Optional.ofNullable(TerminalToolWindowManager.findWidgetByContent(content)))
                .ifPresent(widget -> widget.getTtyConnectorAccessor().executeWithTtyConnector(agent -> typedInto(agent, request)));
    }

    private static @NotNull Stream<Content> tabsNamed(final @NotNull Optional<ToolWindow> window, final @NotNull String name) {
        return window.stream()
                .flatMap(terminal -> Arrays.stream(terminal.getContentManager().getContents()))
                .filter(content -> name.equals(content.getDisplayName()));
    }

    private static void typedInto(final @NotNull TtyConnector agent, final @NotNull String request) {
        try {
            agent.write(request);
        } catch (final IOException ex) {
            TestinLogger.warn("The request could not be typed into the agent's tab: " + FailureText.of(ex));
        }
    }

    private static @NotNull Optional<TtyConnector> connectorOf(final @NotNull Content content) {
        return Optional.ofNullable(TerminalToolWindowManager.findWidgetByContent(content)).flatMap(widget -> Optional.ofNullable(widget.getTtyConnector()));
    }
}
