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

package org.testin.editor.toolbar;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.status.TestRunStatus;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;

public class TestRunResultAnalysisBtn extends AbstractIconButton implements ToolbarItem {
    private final @NotNull TestRunEditor editor;

    // UC-EDITOR-PANEL-045
    public TestRunResultAnalysisBtn(final @NotNull TestRunEditor editor, final @NotNull Runnable onTestRunResultAnalysisClicked) {
        super(Bundle.message("toolbar.analysis"), AllIcons.Actions.ProjectWideAnalysisOff);
        this.editor = editor;

        addActionListener(_ -> onTestRunResultAnalysisClicked.run());
        updateEnabledState();
    }

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-189
    public void updateEnabledState() {
        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        final boolean shown = status == TestRunStatus.COMPLETED || status.isRecord();

        setEnabled(shown);
        describe(shown
                ? Bundle.message("toolbar.analysis")
                : Bundle.message("toolbar.analysis.disabled", status.getLabel()));
    }
}
