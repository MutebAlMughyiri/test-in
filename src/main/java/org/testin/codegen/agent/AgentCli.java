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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.FailureText;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class AgentCli {
    private static final @NotNull Path ANYWHERE = Path.of(System.getProperty("java.io.tmpdir"));

    private static final @NotNull String VERSION = "--version";

    private static final @NotNull String PROMPT_PLACEHOLDER = "{prompt}";

    private final @NotNull Launcher launcher;

    public static @NotNull AgentCli onPath(final @NotNull ProgressIndicator indicator) {
        return new AgentCli((command, arguments, input, timeout) -> start(command, arguments, input, timeout, indicator));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084
    static @NotNull List<String> arguments(final @NotNull String arguments, final @NotNull String prompt) {
        final @NotNull List<String> written = new ArrayList<>(arguments.isBlank() ? List.of() : List.of(arguments.trim().split("\\s+")));

        final int placeholder = written.indexOf(PROMPT_PLACEHOLDER);
        if (placeholder != -1) written.set(placeholder, prompt);

        return List.copyOf(written);
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084
    static boolean wantsThePromptAsAnArgument(final @NotNull String arguments) {
        return arguments.contains(PROMPT_PLACEHOLDER);
    }

    // UC-CODEGEN-021, Rule-CODEGEN-085
    public static @NotNull String triedNames(final @NotNull String command) {
        if (!SystemInfo.isWindows || command.contains(".")) return command;

        return PathEnvironmentVariableUtil.getWindowsExecutableFileExtensions().stream()
                .map(extension -> command + extension)
                .collect(Collectors.joining(", "));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    static @NotNull String executable(final @NotNull String command) {
        return Optional.ofNullable(PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(command)).map(File::getAbsolutePath).orElse(command);
    }

    private static @NotNull Optional<ProcessOutput> start(final @NotNull String command, final @NotNull List<String> arguments, final @NotNull Optional<Path> input, final @NotNull Duration timeout, final @NotNull ProgressIndicator indicator) {
        final @NotNull GeneralCommandLine line = new GeneralCommandLine(executable(command))
                .withParameters(arguments)
                .withWorkingDirectory(ANYWHERE)
                .withCharset(StandardCharsets.UTF_8)
                .withInput(input.map(Path::toFile).orElse(null));

        Logger.debug("Asking the agent: " + line.getCommandLineString());

        try {
            return Optional.of(new CapturingProcessHandler(line).runProcessWithProgressIndicator(indicator, (int) timeout.toMillis()));
        } catch (final ExecutionException ex) {
            Logger.warn("The agent '" + command + "' could not be started: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084
    private static @NotNull Optional<Path> promptFile(final @NotNull String prompt) {
        try {
            final @NotNull Path written = Files.createTempFile("testin-prompt", ".txt");
            Files.writeString(written, prompt, StandardCharsets.UTF_8);

            return Optional.of(written);
        } catch (final IOException ex) {
            Logger.warn("The prompt could not be written for the agent: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    private static void forget(final @NotNull Path written) {
        try {
            Files.deleteIfExists(written);
        } catch (final IOException ex) {
            Logger.warn("The prompt file stayed behind at " + written + ": " + FailureText.of(ex));
        }
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084
    public @NotNull Optional<String> ask(final @NotNull AgentConnection connection, final @NotNull String prompt) {
        if (!connection.isConnected()) return Optional.empty();

        final @NotNull List<String> arguments = arguments(connection.arguments(), prompt);
        if (wantsThePromptAsAnArgument(connection.arguments()))
            return said(connection.command(), arguments, Optional.empty(), connection.timeout());

        final @NotNull Optional<Path> written = promptFile(prompt);
        try {
            return said(connection.command(), arguments, written, connection.timeout());
        } finally {
            written.ifPresent(AgentCli::forget);
        }
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084
    public @NotNull Optional<String> check(final @NotNull AgentConnection connection) {
        if (!connection.isConnected()) return Optional.empty();

        return said(connection.command(), List.of(VERSION), Optional.empty(), connection.timeout());
    }

    private @NotNull Optional<String> said(final @NotNull String command, final @NotNull List<String> arguments, final @NotNull Optional<Path> input, final @NotNull Duration timeout) {
        return launcher.run(command, arguments, input, timeout).map(output -> {
            if (output.getExitCode() != 0)
                Logger.warn("The agent '" + command + "' ended with " + output.getExitCode() + ": " + output.getStderr().strip());

            return output.getStdout().strip();
        });
    }

    @FunctionalInterface
    interface Launcher {
        @NotNull Optional<ProcessOutput> run(@NotNull String command, @NotNull List<String> arguments, @NotNull Optional<Path> input, @NotNull Duration timeout);
    }
}
