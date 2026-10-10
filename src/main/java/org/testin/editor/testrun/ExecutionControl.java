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

package org.testin.editor.testrun;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.Icon;

// UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-035
@Getter
@AllArgsConstructor
public enum ExecutionControl {
    START(
            Bundle.message("toolbar.start.manual.execution"),
            AllIcons.Toolwindows.ToolWindowRun
    ),

    STOP(
            Bundle.message("toolbar.stop.execution"),
            AllIcons.Debugger.ThreadFrozen
    );

    private final @NotNull String label;
    private final @NotNull Icon icon;
}
