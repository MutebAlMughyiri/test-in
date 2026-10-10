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

package org.testin.lightmode;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.card.CardHoverAction;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.util.Bundle;


@Getter
@AllArgsConstructor
public enum LightModePart implements ToolBarAttribute {
    SET_NAME(
            Bundle.message("light.part.set.name"),
            ToolBarDefault.ON,
            ImmutableList.of()
    ),

    DURATION(
            Bundle.message("light.part.duration"),
            ToolBarDefault.ON,
            ImmutableList.of()
    ),

    RUN_ITEM_STATUS_BUTTONS(
            Bundle.message("light.part.run.item.status.buttons"),
            ToolBarDefault.ON,
            ImmutableList.of()
    ),

    STATUS_BAR(
            Bundle.message("light.part.status.bar"),
            ToolBarDefault.ON,
            ImmutableList.of()
    ),

    TEST_METHOD_BUTTON(
            Bundle.message("action.Testin.NavigateToTestMethod.text"),
            ToolBarDefault.ON,
            ImmutableList.of(CardHoverAction.NAVIGATE_TO_TEST_METHOD)
    ),

    RUN_BUTTON(
            Bundle.message("action.Testin.RunTestMethod.text"),
            ToolBarDefault.ON,
            ImmutableList.of(CardHoverAction.RUN_TEST_METHOD, CardHoverAction.STOP_TEST_METHOD)
    ),

    TEST_CASE_BUTTON(
            Bundle.message("action.Testin.NavigateToTestCase.text"),
            ToolBarDefault.ON,
            ImmutableList.of(CardHoverAction.NAVIGATE_TO_TEST_CASE)
    );

    private final @NotNull String name;

    private final @NotNull ToolBarDefault toolBarDefault;

    @Getter(AccessLevel.NONE)
    private final @NotNull ImmutableList<CardHoverAction> buttons;

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-244
    public boolean governs(final @NotNull CardHoverAction button) {
        return buttons.contains(button);
    }
}
