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

package org.testin.ui.framework;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.services.Services;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShownDialog {

    public static boolean isOpen(final @NotNull Project project, final @NotNull Class<?> kind) {
        return Services.getInstance(project, DialogsOnScreen.class).shown(kind).isPresent();
    }

    public static @NotNull JBPopup popup(final @NotNull Project project, final @NotNull Class<?> kind) {
        return Services.getInstance(project, DialogsOnScreen.class).shown(kind).orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " is open"));
    }

    public static @NotNull JComponent content(final @NotNull Project project, final @NotNull Class<?> kind) {
        return popup(project, kind).getContent();
    }

    public static @NotNull JComponent waitedFor(final @NotNull Project project, final @NotNull Class<?> kind) {
        Await.until(kind.getSimpleName() + " never opened", () -> isOpen(project, kind));
        return content(project, kind);
    }

    public static void open(final @NotNull Project project, final @NotNull Class<?> kind, final @NotNull Runnable opening) {
        opening.run();
        sized(project, kind);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    public static void sized(final @NotNull Project project, final @NotNull Class<?> kind) {
        popup(project, kind).setSize(new Dimension(800, 600));
    }

    public static void typed(final @NotNull Project project, final @NotNull Class<?> kind, final @NotNull String text) {
        Drawn.first(content(project, kind), JTextComponent.class, JTextComponent::isEditable).setText(text);
    }

    public static void press(final @NotNull Project project, final @NotNull Class<?> kind, final @NotNull Shortcuts key) {
        press(project, kind, key.getKey());
    }

    public static void press(final @NotNull Project project, final @NotNull Class<?> kind, final @NotNull KeyStroke stroke) {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JBPopup popup = popup(project, kind);
        if (stroke.getModifiers() == 0 && popup.dispatchKeyEvent(new KeyEvent(popup.getContent(), KeyEvent.KEY_PRESSED, 0, 0, stroke.getKeyCode(), KeyEvent.CHAR_UNDEFINED))) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            return;
        }
        press(popup.getContent(), stroke);
    }

    public static void press(final @NotNull JComponent root, final @NotNull KeyStroke stroke) {
        final @NotNull List<JComponent> everywhere = UIUtil.uiTraverser(root).filter(JComponent.class).toList();
        for (final JComponent component : everywhere) {
            for (final AnAction action : ActionUtil.getActions(component)) {
                if (!answers(action, stroke)) continue;
                final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action);
                ActionUtil.updateAction(action, e);
                if (!e.getPresentation().isEnabled()) continue;
                ActionUtil.performAction(action, e);
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
                return;
            }
        }
        final @NotNull Action bound = everywhere.stream().map(component -> bindingOf(component, stroke)).flatMap(Optional::stream).findFirst()
                .orElseThrow(() -> new AssertionError("nothing on the dialog answers " + stroke));
        bound.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, String.valueOf(stroke)));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    public static void close(final @NotNull Project project, final @NotNull Class<?> kind) {
        Services.getInstance(project, DialogsOnScreen.class).shown(kind).ifPresent(JBPopup::cancel);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    public static @NotNull List<DialogPart<?>> componentsOf(final @NotNull AbstractFrameworkDialog dialog) {
        return List.copyOf(dialog.components);
    }

    public static @NotNull List<String> wordsOf(final @NotNull AbstractFrameworkDialog dialog) {
        return dialog.components.stream().flatMap(holder -> Drawn.words(holder.getComponent().getPanel()).stream()).map(word -> StringUtil.removeHtmlTags(word).trim()).toList();
    }

    private static @NotNull Optional<Action> bindingOf(final @NotNull JComponent component, final @NotNull KeyStroke stroke) {
        return boundIn(component, stroke, JComponent.WHEN_FOCUSED)
                .or(() -> boundIn(component, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT))
                .or(() -> boundIn(component, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW));
    }

    private static @NotNull Optional<Action> boundIn(final @NotNull JComponent component, final @NotNull KeyStroke stroke, @MagicConstant(intValues = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_IN_FOCUSED_WINDOW}) final int condition) {
        return Optional.ofNullable(component.getInputMap(condition).get(stroke)).map(name -> component.getActionMap().get(name));
    }

    private static boolean answers(final @NotNull AnAction action, final @NotNull KeyStroke stroke) {
        final Shortcut @NotNull [] shortcuts = action.getShortcutSet().getShortcuts();
        return Arrays.stream(shortcuts).anyMatch(shortcut -> shortcut instanceof final KeyboardShortcut key && stroke.equals(key.getFirstKeyStroke()) && key.getSecondKeyStroke() == null);
    }
}
