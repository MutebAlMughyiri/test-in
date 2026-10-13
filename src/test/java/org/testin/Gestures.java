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

package org.testin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionWrapper;
import com.intellij.openapi.actionSystem.CustomizedDataContext;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Gestures {

    public static @NotNull AnAction boundTo(final @NotNull JComponent on, final @NotNull Class<? extends AnAction> kind) {
        final @NotNull List<AnAction> bound = ActionUtil.getActions(on);
        return bound.stream()
                .filter(action -> kind.isInstance(action) || (action instanceof final AnActionWrapper wrapper && kind.isInstance(wrapper.getDelegate())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " is bound to the " + on.getClass().getSimpleName() + ": " + bound));
    }

    public static @NotNull DataContext dataOf(final @NotNull Project project, final @NotNull JComponent on) {
        final @NotNull DataContext dataContext = SimpleDataContext.getProjectContext(project);
        for (Component at = on; at != null; at = at.getParent()) {
            if (at instanceof final UiDataProvider provider)
                return CustomizedDataContext.withSnapshot(dataContext, sink -> sink.uiDataSnapshot(provider));
        }
        return dataContext;
    }

    public static @NotNull Presentation updated(final @NotNull Project project, final @NotNull AnAction action, final @NotNull JComponent on) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, dataOf(project, on));
        ActionUtil.updateAction(action, e);
        return e.getPresentation();
    }

    public static void press(final @NotNull Project project, final @NotNull AnAction action, final @NotNull JComponent on) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, dataOf(project, on));
        ActionUtil.updateAction(action, e);
        if (e.getPresentation().isEnabled()) ActionUtil.performAction(action, e);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    public static void press(final @NotNull Project project, final @NotNull JComponent on, final @NotNull Class<? extends AnAction> kind) {
        press(project, boundTo(on, kind), on);
    }
}
