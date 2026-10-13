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

package org.testin.filter;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sorting {
    public static void choose(final @NotNull SortPopupButton sort, final @NotNull String label) {
        final @NotNull AnAction entry = entry(sort, label);
        ((ToggleAction) entry).setSelected(TestActionEvent.createTestEvent(entry), true);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    static @NotNull AnAction entry(final @NotNull SortPopupButton sort, final @NotNull String label) {
        return MenuChildren.of(sort.menu()).stream()
                .filter(action -> label.equals(Objects.requireNonNullElse(action.getTemplatePresentation().getText(), "")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the Sort menu has no " + label));
    }

    static void press(final @NotNull SortPopupButton sort, final @NotNull String label) {
        final @NotNull AnAction entry = entry(sort, label);
        ActionUtil.performAction(entry, TestActionEvent.createTestEvent(entry));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }
}
