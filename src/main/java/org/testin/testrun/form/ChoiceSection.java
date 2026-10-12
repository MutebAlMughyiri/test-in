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
package org.testin.testrun.form;

import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.RadioSelection;

import java.util.Map;

public record ChoiceSection(@NotNull TestRunConfiguration field, @NotNull DialogPart<RadioSelection<String>> component) implements TestRunSection {
    // UC-TREE-PANEL-022, Rule-TREE-PANEL-121
    public static @NotNull ChoiceSection of(final @NotNull TestRunConfiguration field, final @NotNull String answer, final @NotNull Runnable changed) {
        final @NotNull DialogPart<RadioSelection<String>> radios = DialogPart.<String>radios(field.getDisplayName())
                .options(field.getOptions(), option -> option)
                .select(answer)
                .build();

        radios.getComponent().onChange(changed);

        return new ChoiceSection(field, radios);
    }

    public @NotNull String value() {
        return component.getComponent().getSelected().trim();
    }

    // Rule-TREE-PANEL-120
    public void showIf(final boolean shown) {
        component.getComponent().getPanel().setVisible(shown);
    }

    // Rule-TREE-PANEL-121
    public boolean needsAnAnswer() {
        return isShown() && !component.getComponent().isAnswered();
    }

    // Rule-TREE-PANEL-120
    @Override
    public void applyTo(final @NotNull Map<TestRunConfiguration, String> answers) {
        answers.put(field, isShown() ? value() : "");
    }

    private boolean isShown() {
        return component.getComponent().getPanel().isVisible();
    }
}
