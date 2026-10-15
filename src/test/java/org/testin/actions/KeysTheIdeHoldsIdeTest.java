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

package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.util.Shortcuts;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class KeysTheIdeHoldsIdeTest extends AbstractIdeTest {

    private static final @NotNull Map<String, Shortcuts> SURFACE_KEYS = new LinkedHashMap<>(Map.of(
            "Testin.Open", Shortcuts.Enter,
            "Testin.ViewDetails", Shortcuts.Enter,
            "Testin.CopyNode", Shortcuts.CopyItem,
            "Testin.CutNode", Shortcuts.CutItem,
            "Testin.PasteNode", Shortcuts.PasteItem,
            "Testin.RemoveNode", Shortcuts.DeletePackage,
            "Testin.CopyTestCase", Shortcuts.CopyItem,
            "Testin.RemoveTestCase", Shortcuts.DeletePackage));

    private static final @NotNull List<String> FIRE_WHERE_THE_IDE_DOES = List.of("Testin.AutomateTestCase", "Testin.Search");

    private static @NotNull List<String> everyTestinAction() {
        return ActionManager.getInstance().getActionIdList("Testin.");
    }

    private static @NotNull List<KeyStroke> keysOf(final @NotNull Keymap keymap, final @NotNull String actionId) {
        return Arrays.stream(keymap.getShortcuts(actionId)).filter(KeyboardShortcut.class::isInstance).map(KeyboardShortcut.class::cast).map(KeyboardShortcut::getFirstKeyStroke).toList();
    }

    private static boolean isASurfaceGesture(final @NotNull KeyStroke key) {
        final int modifiers = key.getModifiers() & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        final boolean bare = modifiers == 0;
        final boolean clipboard = modifiers == Shortcuts.menuMask() && List.of(KeyEvent.VK_C, KeyEvent.VK_X, KeyEvent.VK_V).contains(key.getKeyCode());
        final boolean gesture = List.of(KeyEvent.VK_ENTER, KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE).contains(key.getKeyCode());
        final boolean letter = key.getKeyCode() >= KeyEvent.VK_A && key.getKeyCode() <= KeyEvent.VK_Z;
        return (bare && (gesture || letter)) || clipboard;
    }

    private static @NotNull String pluginXml() {
        final @NotNull Path path = Path.of("src", "main", "resources", "META-INF", "plugin.xml");
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + path.toAbsolutePath(), ex);
        }
    }

    private static @NotNull String declarationOf(final @NotNull String xml, final @NotNull String actionId) {
        final int from = xml.indexOf("id=\"" + actionId + "\"");
        assertTrue("plugin.xml does not declare " + actionId, from >= 0);
        final int next = xml.indexOf("<action", from);
        return xml.substring(xml.lastIndexOf("<action", from), next >= 0 ? next : xml.length());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-066
    public void testARebindableKeyIsDeclaredToTheIdeAndASurfacesOwnGestureIsRegisteredOnTheSurface() {
        final @NotNull Keymap keymap = KeymapManager.getInstance().getActiveKeymap();

        final @NotNull List<String> gesturesTaken = new ArrayList<>();
        for (final String actionId : everyTestinAction()) {
            keysOf(keymap, actionId).stream().filter(KeysTheIdeHoldsIdeTest::isASurfaceGesture).forEach(key -> gesturesTaken.add(actionId + " " + KeymapUtil.getKeystrokeText(key)));
        }
        assertEquals("a surface's own gesture is declared to the IDE, where it is dispatched before the tree, the card list or the grid", List.of(), gesturesTaken);
        assertFalse("the search's key is not declared to the IDE, so the Keymap cannot rebind it", keysOf(keymap, "Testin.Search").isEmpty());

        for (final Map.Entry<String, Shortcuts> surface : SURFACE_KEYS.entrySet()) {
            final @NotNull AnAction action = Declared.action(surface.getKey());
            assertFalse("Find Action cannot offer " + surface.getKey() + " by name", Objects.toString(action.getTemplatePresentation().getText(), "").isBlank());

            final @NotNull JPanel tree = new JPanel();
            Declared.bindTo(surface.getKey(), tree);
            assertTrue(surface.getKey() + " is not registered on the surface it belongs to", ActionUtil.getActions(tree).stream()
                    .anyMatch(bound -> surface.getValue().is(bound.getShortcutSet())));
        }
    }

    // UC-INTERNAL-001, Rule-INTERNAL-068
    public void testAKeyASurfaceGivesAnActionIsBoundOnACopyAndTheTooltipReadsItFromTheSamePlace() {
        for (final Map.Entry<String, Shortcuts> surface : SURFACE_KEYS.entrySet()) {
            final @NotNull AnAction held = Declared.action(surface.getKey());
            final @NotNull List<Shortcut> shipped = List.of(held.getShortcutSet().getShortcuts());

            final @NotNull AnAction inTheMenu = Declared.forMenu(surface.getKey());
            Declared.bindTo(surface.getKey(), new JPanel());

            assertNotSame("the menu carries the action the IDE holds, not a copy of it", held, inTheMenu);
            assertEquals("binding " + surface.getKey() + " on a surface changed the action the IDE holds", shipped, List.of(held.getShortcutSet().getShortcuts()));
            assertTrue("the copy in the menu does not carry the key the surface gives it", surface.getValue().is(inTheMenu.getShortcutSet()));
            assertEquals("the tooltip prints a different key from the one the binding uses", KeymapUtil.getFirstKeyboardShortcutText(inTheMenu.getShortcutSet()), Declared.shortcutText(surface.getKey()));
        }
    }

    // Rule-INTERNAL-069
    public void testADefaultKeyTheIdeAlreadyUsesIsTakenOnlyWhereThePluginDescriptorSaysWhichAndWhy() {
        final @NotNull Keymap defaults = Optional.ofNullable(KeymapManagerEx.getInstanceEx().getKeymap("$default")).orElseThrow(() -> new AssertionError("the IDE has no default keymap"));
        final @NotNull String xml = pluginXml();

        final @NotNull List<String> noted = new ArrayList<>();
        for (final String actionId : everyTestinAction()) {
            final boolean shared = keysOf(defaults, actionId).stream()
                    .anyMatch(key -> Arrays.stream(defaults.getActionIds(key)).anyMatch(other -> !other.startsWith("Testin.")));
            if (shared && declarationOf(xml, actionId).contains("Rule-INTERNAL-069")) noted.add(actionId);
        }
        assertEquals("the keys the IDE's action and Testin's can both fire in one place are not exactly the ones noted beside their binding", FIRE_WHERE_THE_IDE_DOES, noted.stream().sorted().toList());

        final @NotNull Keymap mac = Optional.ofNullable(KeymapManagerEx.getInstanceEx().getKeymap("Mac OS X 10.5+")).orElseThrow(() -> new AssertionError("the IDE has no Mac keymap"));
        final @NotNull KeyStroke macKey = KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK);
        assertEquals("Automate Test Case on a Mac answers a key other than its own", List.of(macKey), keysOf(mac, "Testin.AutomateTestCase"));
        assertEquals("Automate Test Case's Mac key is one the IDE already uses", List.of("Testin.AutomateTestCase"), List.of(mac.getActionIds(macKey)));
    }
}
