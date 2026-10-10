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

package org.testin.bug;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.ui.Badge;
import org.testin.ui.Pill;
import org.testin.util.Bundle;

import java.awt.Color;
import java.util.List;

public record BugIssueState(@NotNull String label, @NotNull Color color, @NotNull String tooltip) {
    public static final @NotNull BugIssueState NOT_READ = new BugIssueState("", JBColor.GRAY, "");
    static final @NotNull BugIssueState OPEN = new BugIssueState(Bundle.message("bug.state.open"), RunItemStatus.FAILED.getRowColor(), "");
    static final @NotNull BugIssueState FIXED = new BugIssueState(Bundle.message("bug.state.fixed"), RunItemStatus.PASSED.getRowColor(), "");
    static final @NotNull BugIssueState NOT_PLANNED = new BugIssueState(Bundle.message("bug.state.not.planned"), JBColor.GRAY, "");
    static final @NotNull BugIssueState DUPLICATE = new BugIssueState(Bundle.message("bug.state.not.planned"), JBColor.GRAY, Bundle.message("bug.state.duplicate.tooltip"));

    static @NotNull BugIssueState onBoard(final @NotNull String column, final @NotNull String colorName, final @NotNull String project) {
        return new BugIssueState(column, BoardColor.of(colorName), Bundle.message("bug.state.board.tooltip", project));
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-091
    public @NotNull List<Badge> pills() {
        return label.isBlank() ? List.of() : List.of(new Pill(label, color));
    }
}
