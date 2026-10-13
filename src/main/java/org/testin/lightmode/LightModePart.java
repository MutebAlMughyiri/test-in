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
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.model.ShowableField;
import org.testin.model.FieldDefault;
import org.testin.util.Bundle;


@Getter
@AllArgsConstructor
public enum LightModePart implements ShowableField {
    SET_NAME(
            Bundle.message("light.part.set.name"),
            FieldDefault.ON,
            ImmutableList.of()
    ),

    DURATION(
            Bundle.message("light.part.duration"),
            FieldDefault.ON,
            ImmutableList.of()
    ),

    RUN_ITEM_STATUS_BUTTONS(
            Bundle.message("light.part.run.item.status.buttons"),
            FieldDefault.ON,
            ImmutableList.of()
    ),

    STATUS_BAR(
            Bundle.message("light.part.status.bar"),
            FieldDefault.ON,
            ImmutableList.of()
    ),

    TEST_METHOD_BUTTON(
            Bundle.message("action.Testin.NavigateToTestMethod.text"),
            FieldDefault.ON,
            ImmutableList.of(TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD)
    ),

    RUN_BUTTON(
            Bundle.message("action.Testin.RunTestMethod.text"),
            FieldDefault.ON,
            ImmutableList.of(TestCaseQuickAction.RUN_TEST_METHOD, TestCaseQuickAction.STOP_TEST_METHOD)
    ),

    TEST_CASE_BUTTON(
            Bundle.message("action.Testin.NavigateToTestCase.text"),
            FieldDefault.ON,
            ImmutableList.of(TestCaseQuickAction.NAVIGATE_TO_TEST_CASE)
    );

    private final @NotNull String name;

    private final @NotNull FieldDefault toolBarDefault;

    @Getter(AccessLevel.NONE)
    private final @NotNull ImmutableList<TestCaseQuickAction> buttons;

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-244
    public boolean governs(final @NotNull TestCaseQuickAction button) {
        return buttons.contains(button);
    }
}
