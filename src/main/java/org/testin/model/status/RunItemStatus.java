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
import org.testin.model.RunItemStatusKey;
import org.testin.model.ReportColor;
import org.testin.util.Bundle;
import org.testin.util.FixedColors;

import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.KeyEvent;

@Getter
@AllArgsConstructor
public enum RunItemStatus {
    PASSED(
            JBColor.GREEN,
            FixedColors.GREEN.darker(),
            Bundle.message("status.run.item.passed"),
            new RunItemStatusKey(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)),
            false
    ),

    FAILED(
            JBColor.RED.darker(),
            FixedColors.RED.darker(),
            Bundle.message("status.run.item.failed"),
            new RunItemStatusKey(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)),
            true
    ),

    BLOCKED(
            JBColor.ORANGE,
            FixedColors.ORANGE.darker(),
            Bundle.message("status.run.item.blocked"),
            new RunItemStatusKey(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)),
            false
    ),

    PENDING(
            JBColor.lazy(UIUtil::getContextHelpForeground),
            FixedColors.GRAY.darker(),
            Bundle.message("status.run.item.pending"),
            RunItemStatusKey.NONE,
            false
    ),

    REMOVED(
            JBColor.GRAY,
            FixedColors.GRAY.darker(),
            Bundle.message("status.run.item.removed"),
            RunItemStatusKey.NONE,
            false
    ),

    UNTESTED(
            JBColor.GRAY.brighter(),
            FixedColors.GRAY.darker(),
            Bundle.message("status.run.item.untested"),
            RunItemStatusKey.NONE,
            false
    );

    private final @NotNull Color rowColor;
    private final @NotNull Color reportColor;
    private final @NotNull String label;

    private final @NotNull RunItemStatusKey menuEntry;

    private final boolean collectsFailureDetails;

    // Rule-REPORT-025
    public @NotNull String getReportHex() {
        return ColorUtil.toHex(reportColor);
    }

    // Rule-REPORT-025
    public @NotNull String getReportDarkHex() {
        return ColorUtil.toHex(ReportColor.forDarkPage(reportColor));
    }

    public boolean isRunItemStatus() {
        return !menuEntry.equals(RunItemStatusKey.NONE);
    }
}
