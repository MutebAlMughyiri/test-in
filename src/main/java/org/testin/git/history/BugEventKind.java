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

package org.testin.git.history;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Bundle;

import java.awt.Color;

// Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-108
@Getter
@AllArgsConstructor
public enum BugEventKind implements CardKind {
    RECORDED(
            RunItemStatus.FAILED.getRowColor(),
            true
    ),

    CHANGED(
            JBColor.ORANGE,
            true
    ),

    CLEARED(
            RunItemStatus.PASSED.getRowColor(),
            false
    ),

    REMOVED(
            RunItemStatus.PASSED.getRowColor(),
            false
    );

    private final @NotNull Color color;

    private final boolean open;

    // Rule-VIEW-PANEL-115
    @Override
    public @NotNull String getLabel() {
        return Bundle.message("view.history.bug");
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-108
    public @NotNull Color barOf(final @NotNull RunItem runItem) {
        return open && runItem.isFailed() ? runItem.getBugSeverity().getColor() : color;
    }
}
