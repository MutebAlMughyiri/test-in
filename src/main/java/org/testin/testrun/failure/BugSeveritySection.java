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

import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.RadioSelection;

import java.util.List;

public record BugSeveritySection(@NotNull DialogPart<RadioSelection<BugSeverity>> component) implements FailureSection {
    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148, Rule-EDITOR-PANEL-147
    public static @NotNull BugSeveritySection of(final @NotNull RunItem runItem) {
        return new BugSeveritySection(DialogPart.<BugSeverity>radios(TestRunEditorFields.BUG_SEVERITY.getName())
                .options(List.of(BugSeverity.values()), BugSeverity::getLabel)
                .select(runItem.shownExecution().getBugSeverity())
                .build());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    public @NotNull BugSeverity selected() {
        return component.getComponent().getSelected();
    }
}
