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

package org.testin.robot;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.ui.components.JBLabel;
import lombok.AccessLevel;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestName;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

// UC-CODEGEN-008, Rule-CODEGEN-120
public final class RobotTestsConfiguration extends RunConfigurationBase<Object> {
    private static final @NotNull String SUITES = "suites";

    private static final @NotNull String TEST_CASES = "testCases";

    private @NotNull List<String> suites = List.of();

    @Getter(AccessLevel.PACKAGE)
    private @NotNull List<UUID> testCaseUuids = List.of();

    RobotTestsConfiguration(final @NotNull Project project, final @NotNull ConfigurationFactory factory) {
        super(project, factory, Bundle.message("robot.run.type"));
    }

    private static @NotNull List<String> linesOf(final @NotNull Element element, final @NotNull String field) {
        return Optional.ofNullable(JDOMExternalizerUtil.readField(element, field)).stream().flatMap(String::lines).filter(line -> !line.isBlank()).toList();
    }

    // Rule-CODEGEN-031
    void toRun(final @NotNull List<UUID> testCases, final @NotNull List<String> suiteFiles) {
        testCaseUuids = List.copyOf(testCases);
        suites = List.copyOf(suiteFiles);
    }

    // Rule-CODEGEN-120
    @NotNull GeneralCommandLine commandLine(final @NotNull String interpreter, final @NotNull Path output) {
        final @NotNull Stream<String> robot = Stream.of("-m", "robot", "--outputdir", output.toString(), "--report", "NONE", "--log", "NONE", "--consolecolors", "ansi");
        final @NotNull Stream<String> chosen = testCaseUuids.stream().flatMap(testCaseUuid -> Stream.of("--include", testCaseUuid.toString()));
        final @NotNull GeneralCommandLine commandLine = new GeneralCommandLine(interpreter)
                .withParameters(Stream.of(robot, chosen, suites.stream()).flatMap(arguments -> arguments).toList())
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment("PYTHONIOENCODING", "utf-8");
        Optional.ofNullable(getProject().getBasePath()).ifPresent(commandLine::setWorkDirectory);

        return commandLine;
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SettingsEditor<RobotTestsConfiguration>() {
            private final @NotNull JBLabel chosen = new JBLabel();

            @Override
            protected void resetEditorFrom(final @NotNull RobotTestsConfiguration configuration) {
                chosen.setText(Bundle.message("robot.run.chosen", String.valueOf(configuration.testCaseUuids.size()), String.join(", ", configuration.suites)));
            }

            @Override
            protected void applyEditorTo(final @NotNull RobotTestsConfiguration configuration) {
            }

            @Override
            protected @NotNull JComponent createEditor() {
                return chosen;
            }
        };
    }

    @Override
    public @NotNull RunProfileState getState(final @NotNull Executor executor, final @NotNull ExecutionEnvironment environment) {
        return new RobotTestsState(environment, this);
    }

    @Override
    public void readExternal(final @NotNull Element element) {
        super.readExternal(element);
        suites = linesOf(element, SUITES);
        testCaseUuids = linesOf(element, TEST_CASES).stream().map(TestName::testCaseUuid).flatMap(Optional::stream).toList();
    }

    @Override
    public void writeExternal(final @NotNull Element element) {
        super.writeExternal(element);
        JDOMExternalizerUtil.writeField(element, SUITES, String.join("\n", suites));
        JDOMExternalizerUtil.writeField(element, TEST_CASES, String.join("\n", testCaseUuids.stream().map(UUID::toString).toList()));
    }
}
