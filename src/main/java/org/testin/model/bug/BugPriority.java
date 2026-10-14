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

package org.testin.model.bug;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportEmphasis;
import org.testin.util.Bundle;

import java.awt.Color;

@Getter
@AllArgsConstructor
public enum BugPriority {
    HIGH(
            Bundle.message("bug.priority.high"),
            JBColor.RED.brighter().brighter(),
            ReportEmphasis.ALARMING,
            "🔴 High"
    ),

    MEDIUM(
            Bundle.message("bug.priority.medium"),
            JBColor.BLUE.brighter(),
            ReportEmphasis.CAUTIONARY,
            "🔵 Medium"
    ),

    LOW(
            Bundle.message("bug.priority.low"),
            JBColor.GRAY.brighter(),
            ReportEmphasis.MUTED,
            "⚪ Low"
    );

    // Rule-EDITOR-PANEL-147
    public static final @NotNull BugPriority DEFAULT = LOW;

    private final @NotNull String label;

    private final @NotNull Color color;

    private final @NotNull ReportEmphasis emphasis;

    private final @NotNull String inBugReport;
}
