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
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.Map;

public record CommitSection(@NotNull DialogPart<TextInput> component) implements TestRunSection {
    // UC-TREE-PANEL-021
    public static @NotNull CommitSection of(final @NotNull String value) {
        return new CommitSection(DialogPart.textInput()
                .caption(TestRunConfiguration.COMMIT.getDisplayName())
                .placeholder(Bundle.message("test.run.form.commit.hint"))
                .value(value)
                .build());
    }

    @Override
    public void applyTo(final @NotNull Map<TestRunConfiguration, String> answers) {
        answers.put(TestRunConfiguration.COMMIT, component.getComponent().getText().trim());
    }
}
