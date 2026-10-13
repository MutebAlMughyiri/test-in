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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogHost;
import org.testin.ui.framework.MultiLineField;
import org.testin.util.Bundle;

import java.util.Map;

public record ChangeLogSection(@NotNull DialogPart<MultiLineField> component) implements TestRunSection {
    // UC-TREE-PANEL-021, Rule-INTERNAL-096
    public static @NotNull ChangeLogSection of(final @NotNull Project project, final @NotNull String value) {
        return new ChangeLogSection(DialogPart.multiLineField(project, TestRunConfiguration.CHANGE_LOG.getDisplayName(), Bundle.message("test.run.form.change.log.hint"), value));
    }

    // Rule-TREE-PANEL-122
    public void takesLineBreaks(final @NotNull DialogHost host) {
        component.getComponent().insertsNewLine(host);
        component.getComponent().ignoresEnter(host);
    }

    @Override
    public void applyTo(final @NotNull Map<TestRunConfiguration, String> answers) {
        answers.put(TestRunConfiguration.CHANGE_LOG, component.getComponent().getText().trim());
    }
}
