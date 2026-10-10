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
import com.intellij.openapi.actionSystem.AnActionHolder;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MenuChildren {
    static @NotNull List<AnAction> of(final @NotNull ActionGroup group) {
        final @NotNull List<AnAction> children = group instanceof final DefaultActionGroup plain ? Arrays.asList(plain.getChildActionsOrStubs()) : opened(group);
        return children.stream().filter(child -> !(child instanceof Separator)).toList();
    }

    private static @NotNull List<AnAction> opened(final @NotNull ActionGroup group) {
        final @NotNull ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, group, DataContext.EMPTY_CONTEXT, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
        try {
            final @NotNull List<?> items = popup.getListStep().getValues();
            return items.stream().map(AnActionHolder.class::cast).map(AnActionHolder::getAction).toList();
        } finally {
            Disposer.dispose(popup);
        }
    }
}
