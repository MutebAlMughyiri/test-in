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

import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.ExecutionControl;
import org.testin.editor.testrun.ExecutionMode;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.status.TestRunStatus;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;

public class StartExecutionBtn extends AbstractIconButton implements ToolbarItem {
    private final @NotNull TestRunEditor editor;

    public StartExecutionBtn(final @NotNull TestRunEditor editor, final @NotNull Runnable onStartExecutionClicked) {
        super(ExecutionControl.START.getLabel(), ExecutionControl.START.getIcon());
        this.editor = editor;

        addActionListener(_ -> onStartExecutionClicked.run());
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135, Rule-EDITOR-PANEL-290
    public static @NotNull String tooltipFor(final @NotNull TestRunEditor editor) {
        final @NotNull ExecutionMode mode = editor.getWalk().mode();
        if (mode != ExecutionMode.IDLE) return mode.getRefusal();

        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        if (status.isTerminal()) return Bundle.message("toolbar.execution.disabled", status.getLabel());

        return editor.getWalk().hasSomethingToWalk()
                ? ExecutionControl.START.getLabel()
                : Bundle.message("toolbar.nothing.to.execute");
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public void updateEnabledState() {
        setEnabled(editor.getWalk().canStartManualExecution());
        describe(tooltipFor(editor));
    }
}