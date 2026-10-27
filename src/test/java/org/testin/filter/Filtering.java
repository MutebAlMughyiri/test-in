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

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Filtering {
    public static void choose(final @NotNull FilterPopupButton filter, final @NotNull String submenu, final @NotNull String label) {
        ticked(filter, submenu, label, true);
    }

    public static void untick(final @NotNull FilterPopupButton filter, final @NotNull String submenu, final @NotNull String label) {
        ticked(filter, submenu, label, false);
    }

    private static void ticked(final @NotNull FilterPopupButton filter, final @NotNull String submenu, final @NotNull String label, final boolean ticked) {
        final @NotNull AnAction entry = named(MenuChildren.of((ActionGroup) named(MenuChildren.of(filter.menu()), submenu)), label);
        ((ToggleAction) entry).setSelected(TestActionEvent.createTestEvent(entry), ticked);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static @NotNull AnAction named(final @NotNull List<AnAction> entries, final @NotNull String label) {
        return entries.stream()
                .filter(action -> label.equals(Objects.requireNonNullElse(action.getTemplatePresentation().getText(), "")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the Filter menu has no " + label));
    }
}
