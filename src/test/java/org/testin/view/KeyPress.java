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
package org.testin.view;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.TestActionEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyPress {

    public static @NotNull List<AnAction> answering(final @NotNull JComponent pressedIn, final @NotNull KeyStroke key) {
        final @NotNull List<AnAction> found = new ArrayList<>();
        collect(pressedIn, key, found);
        return found;
    }

    private static void collect(final @NotNull Component holder, final @NotNull KeyStroke key, final @NotNull List<AnAction> found) {
        if (holder instanceof final JComponent component) {
            ActionUtil.getActions(component).stream().filter(action -> answers(action, key)).forEach(found::add);
        }
        Optional.ofNullable(holder.getParent()).ifPresent(parent -> collect(parent, key, found));
    }

    public static @NotNull Optional<AnAction> boundTo(final @NotNull JComponent pressedIn, final @NotNull KeyStroke key) {
        return answering(pressedIn, key).stream().findFirst();
    }

    private static boolean answers(final @NotNull AnAction action, final @NotNull KeyStroke key) {
        return Arrays.stream(action.getShortcutSet().getShortcuts())
                .anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut keyboard && keyboard.getFirstKeyStroke().equals(key) && keyboard.getSecondKeyStroke() == null);
    }

    public static @NotNull AnActionEvent eventIn(final @NotNull Project project, final @NotNull AnAction action, final @NotNull JComponent pressedIn) {
        return TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, pressedIn)
                .build());
    }

    public static boolean press(final @NotNull Project project, final @NotNull JComponent pressedIn, final @NotNull KeyStroke key) {
        final @NotNull AnAction action = boundTo(pressedIn, key).orElseThrow(() -> new AssertionError("nothing answers " + key + " in " + pressedIn.getClass().getSimpleName()));
        final @NotNull AnActionEvent e = eventIn(project, action, pressedIn);

        ActionUtil.updateAction(action, e);
        if (!e.getPresentation().isEnabled()) return false;

        ActionUtil.performAction(action, e);
        return true;
    }
}
