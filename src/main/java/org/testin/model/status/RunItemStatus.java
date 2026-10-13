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

package org.testin.model.status;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportColor;
import org.testin.util.Bundle;
import org.testin.util.FixedColors;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Locale;

@Getter
@AllArgsConstructor
public enum RunItemStatus {
    PASSED(
            JBColor.GREEN,
            FixedColors.GREEN.darker(),
            Bundle.message("status.run.item.passed"),
            KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)
    ),

    FAILED(
            JBColor.RED.darker(),
            FixedColors.RED.darker(),
            Bundle.message("status.run.item.failed"),
            KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)
    ),

    BLOCKED(
            JBColor.ORANGE,
            FixedColors.ORANGE.darker(),
            Bundle.message("status.run.item.blocked"),
            KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)
    ),

    PENDING(
            JBColor.lazy(UIUtil::getContextHelpForeground),
            FixedColors.GRAY.darker(),
            Bundle.message("status.run.item.pending"),
            Shortcuts.NO_KEY
    ),

    REMOVED(
            JBColor.GRAY,
            FixedColors.GRAY.darker(),
            Bundle.message("status.run.item.removed"),
            Shortcuts.NO_KEY
    ),

    UNTESTED(
            JBColor.GRAY.brighter(),
            FixedColors.GRAY.darker(),
            Bundle.message("status.run.item.untested"),
            Shortcuts.NO_KEY
    );

    private final @NotNull Color rowColor;
    private final @NotNull Color reportColor;
    private final @NotNull String label;

    private final @NotNull KeyStroke shortcut;

    // Rule-REPORT-025
    public @NotNull String getReportHex() {
        return ColorUtil.toHex(reportColor);
    }

    // Rule-REPORT-025
    public @NotNull String getReportDarkHex() {
        return ColorUtil.toHex(ReportColor.forDarkPage(reportColor));
    }

    // Rule-REPORT-025
    public @NotNull String getCssVariable() {
        return "--run-item-status-" + name().toLowerCase(Locale.ROOT);
    }

    // Rule-REPORT-025
    public @NotNull String getCssToken() {
        return "var(" + getCssVariable() + ")";
    }

    // Rule-EDITOR-PANEL-176
    public @NotNull String labelIn(final @NotNull TestRunStatus testRunStatus) {
        return this == UNTESTED && !testRunStatus.isTerminal() ? PENDING.label : label;
    }

    // Rule-PRODUCT-012
    public boolean isJudged() {
        return !Shortcuts.isNoKey(shortcut);
    }

    // UC-EDITOR-PANEL-034
    public boolean collectsFailureDetails() {
        return this == FAILED;
    }
}
