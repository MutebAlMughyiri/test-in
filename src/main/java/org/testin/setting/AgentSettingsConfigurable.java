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

package org.testin.setting;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.agent.AgentCli;
import org.testin.codegen.agent.AgentConnection;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.time.Duration;
import java.util.Optional;

// UC-CODEGEN-021, Rule-CODEGEN-083
public final class AgentSettingsConfigurable implements SearchableConfigurable {
    static final @NotNull String ID = "org.testin.setting.AgentSettingsConfigurable";

    private static final @NotNull Duration CHECK_TIMEOUT = Duration.ofSeconds(20);

    private final @NotNull JBTextField commandField = new JBTextField();

    private final @NotNull JBTextField argumentsField = new JBTextField();

    private final @NotNull JBLabel said = new JBLabel("");

    private final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return Bundle.message("agent.page.name");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    @Override
    public @NotNull JComponent createComponent() {
        final @NotNull JButton check = new JButton(Bundle.message("agent.check.button"));
        check.addActionListener(_ -> said.setText(whatTheAgentSaid()));

        final @NotNull JBPanel<?> buttons = new JBPanel<>();
        buttons.setOpaque(false);
        buttons.add(check);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.command")), commandField, 1, false)
                .addComponentToRightColumn(SettingsNote.of(Bundle.message("agent.hint.command")))
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.arguments")), argumentsField, 1, false)
                .addComponentToRightColumn(SettingsNote.of(Bundle.message("agent.hint.arguments")))
                .addVerticalGap(10)
                .addComponent(SettingsNote.of(Bundle.message("agent.keys")))
                .addComponent(said)
                .addComponent(buttons)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-085
    private @NotNull String whatTheAgentSaid() {
        final @NotNull AgentConnection typed = typedConnection();
        if (!typed.isConnected()) return Bundle.message("agent.check.no.command");

        final @NotNull Optional<String> answered = BackgroundWork.synchronously(Bundle.message("agent.check.button"), true, () -> AgentCli.onPath(ProgressManager.getInstance().getProgressIndicator()).check(typed), Optional.empty());
        if (answered.isEmpty())
            return Bundle.message("agent.check.not.found", typed.command(), AgentCli.triedNames(typed.command()));

        return answered.orElseThrow().isBlank()
                ? Bundle.message("agent.check.said.nothing", typed.command())
                : Bundle.message("agent.check.answered", firstLineOf(answered.orElseThrow()));
    }

    private @NotNull String firstLineOf(final @NotNull String answered) {
        return answered.lines().findFirst().orElse(answered).strip();
    }

    private @NotNull AgentConnection typedConnection() {
        return new AgentConnection(commandField.getText().trim(), argumentsField.getText().trim(), "", CHECK_TIMEOUT);
    }

    @Override
    public boolean isModified() {
        return !settings.agentCommand.equals(commandField.getText().trim())
                || !settings.agentArguments.equals(argumentsField.getText().trim());
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    @Override
    public void apply() {
        settings.agentCommand = commandField.getText().trim();
        settings.agentArguments = argumentsField.getText().trim();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    @Override
    public void reset() {
        commandField.setText(settings.agentCommand);
        argumentsField.setText(settings.agentArguments);
        said.setText("");
    }
}
