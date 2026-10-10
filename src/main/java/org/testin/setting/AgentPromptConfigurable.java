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
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.agent.AgentConnection;
import org.testin.codegen.agent.BodyPrompt;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Fonts;

import javax.swing.JButton;
import javax.swing.JComponent;

// UC-CODEGEN-021, Rule-CODEGEN-086
public final class AgentPromptConfigurable implements SearchableConfigurable {
    private static final int PROMPT_ROWS = 10;
    private static final int PROMPT_COLUMNS = 60;

    private final @NotNull JBTextArea promptArea = new JBTextArea(PROMPT_ROWS, PROMPT_COLUMNS);
    private final @NotNull JBTextField timeoutField = new JBTextField();
    private final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

    private static @NotNull JBLabel hint(final @NotNull String text) {
        final @NotNull JBLabel note = new JBLabel(text);

        note.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        note.setFont(Fonts.small());

        return note;
    }

    @Override
    public @NotNull String getId() {
        return "org.testin.setting.AgentPromptConfigurable";
    }

    @Override
    public @NotNull String getDisplayName() {
        return Bundle.message("agent.prompt.page.name");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    @Override
    public @NotNull JComponent createComponent() {
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);

        final @NotNull JButton resetPrompt = new JButton(Bundle.message("agent.reset.button"));
        resetPrompt.addActionListener(_ -> promptArea.setText(Bundle.message("agent.prompt.default")));

        final @NotNull JBPanel<?> buttons = new JBPanel<>();
        buttons.setOpaque(false);
        buttons.add(resetPrompt);

        return FormBuilder.createFormBuilder()
                .addComponent(hint(Bundle.message("agent.sent")))
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.prompt")), new JBScrollPane(promptArea), 1, true)
                .addComponentToRightColumn(hint(Bundle.message("agent.hint.prompt", String.join(" ", BodyPrompt.PLACEHOLDERS))))
                .addComponentToRightColumn(buttons)
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.timeout")), timeoutField, 1, false)
                .addComponentToRightColumn(hint(Bundle.message("agent.hint.timeout")))
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    private int typedTimeout() {
        try {
            return Integer.parseInt(timeoutField.getText().trim());
        } catch (final NumberFormatException notANumber) {
            return AppSettingsState.DEFAULT_TIMEOUT_SECONDS;
        }
    }

    @Override
    public boolean isModified() {
        return !settings.agentPrompt.equals(typedPrompt()) || settings.agentTimeoutSeconds != typedTimeout();
    }

    // Rule-CODEGEN-086
    private @NotNull String typedPrompt() {
        final @NotNull String typed = promptArea.getText().trim();

        return typed.equals(Bundle.message("agent.prompt.default")) ? "" : typed;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    @Override
    public void apply() {
        settings.agentPrompt = typedPrompt();
        settings.agentTimeoutSeconds = typedTimeout() > 0 ? typedTimeout() : AppSettingsState.DEFAULT_TIMEOUT_SECONDS;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    @Override
    public void reset() {
        promptArea.setText(AgentConnection.stored().promptTemplate());
        timeoutField.setText(String.valueOf(settings.agentTimeoutSeconds));
    }
}
