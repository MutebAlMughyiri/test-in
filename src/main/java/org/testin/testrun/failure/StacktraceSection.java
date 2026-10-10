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

package org.testin.testrun.failure;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.MultiLineField;
import org.testin.util.Bundle;

public record StacktraceSection(@NotNull ComponentDialogBase<MultiLineField> component) implements FailureSection {
    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public static @NotNull StacktraceSection of(final @NotNull Project p, final @NotNull RunItem runItem) {
        return new StacktraceSection(ComponentDialogBase.multiLineField(p, TestRunEditorAttributes.STACKTRACE.getName(), Bundle.message("dialog.failure.placeholder.error"), runItem.getStacktrace()));
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    public @NotNull String typed() {
        return component.getComponent().getText().trim();
    }
}
