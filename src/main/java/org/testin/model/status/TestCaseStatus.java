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

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.FixedColors;

import java.awt.Color;

@Getter
@AllArgsConstructor
public enum TestCaseStatus {
    REVIEWED(
            Bundle.message("status.test.case.reviewed"),
            true,
            FixedColors.CREAM,
            false
    ),

    PENDING(
            Bundle.message("status.test.case.pending"),
            true,
            UIUtil.getLabelForeground(),
            false
    ),

    DISABLED(
            Bundle.message("status.test.case.disabled"),
            false,
            SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor(),
            false
    ),

    TO_BE_UPDATED(
            Bundle.message("status.test.case.to.be.updated"),
            true,
            UIUtil.getLabelForeground(),
            true
    );

    private final @NotNull String label;

    // Rule-CODEGEN-047, Rule-CODEGEN-048
    private final boolean enabled;

    // Rule-EDITOR-PANEL-294
    private final @NotNull Color descriptionColor;

    // Rule-EDITOR-PANEL-294
    private final boolean badged;
}
