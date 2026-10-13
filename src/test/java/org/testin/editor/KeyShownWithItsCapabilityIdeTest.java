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

package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.keymap.KeymapUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.card.Offered;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Optional;

public class KeyShownWithItsCapabilityIdeTest extends AbstractCodegenIdeTest {

    // Rule-PRODUCT-016
    public void testAKeyIsShownWhereverTheCapabilityItTriggersIsShown() {
        final @NotNull TestCase testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");

        for (final TestCaseQuickAction button : List.of(TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD, TestCaseQuickAction.RUN_TEST_METHOD)) {
            final @NotNull AnAction action = Optional.ofNullable(ActionManager.getInstance().getAction(button.getActionId())).orElseThrow(() -> new AssertionError(button.getActionId() + " is not registered"));
            final @NotNull String key = KeymapUtil.getFirstKeyboardShortcutText(action);
            assertFalse(button.getTooltip() + " has no key in the menu, so this proves nothing", key.isEmpty());

            final @NotNull Offered onCard = button.offer(getProject(), Optional.empty(), testCase);
            assertTrue("the card offers " + button.getTooltip() + " without the key the menu shows: " + onCard.hintText(), onCard.hintText().endsWith(key));
        }
    }
}
