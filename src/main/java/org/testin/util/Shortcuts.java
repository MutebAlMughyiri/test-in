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

package org.testin.util;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.SystemInfo;
import lombok.Getter;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;

@Getter
public enum Shortcuts {
    EMPTY(
            KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)
    ),

    Enter(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
    ),

    Escape(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    ),

    Corrections(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK)
    ),

    InsertNewLine(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuMask())
    ),

    AddItem(
            KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuMask()),
            KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuMask() | InputEvent.SHIFT_DOWN_MASK),
            KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, menuMask()),
            KeyStroke.getKeyStroke(KeyEvent.VK_ADD, menuMask())
    ),

    RemoveArrayItem(
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK)
    ),

    CaretOnEveryValue(
            KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask() | InputEvent.SHIFT_DOWN_MASK)
    ),

    FocusSearch(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask())
    ),

    CopyItem(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask())
    ),

    CutItem(
            KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask())
    ),

    PasteItem(
            KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask())
    ),

    ToggleDetails(
            KeyStroke.getKeyStroke(KeyEvent.VK_D, menuMask())
    ),

    ContextMenu(
            KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0)
    ),

    Undo(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask())
    ),

    Redo(
            KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask())
    ),

    DeletePackage(
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)
    ),

    GenerateReport(
            KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK))
    ),

    Next(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
    ),

    Previous(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
    ),

    First(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
    ),

    Last(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
    ),

    CopyAll(
            KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)
    ),

    CopyDescription(
            KeyStroke.getKeyStroke(KeyEvent.VK_D, 0)
    ),

    CopyExpectedResult(
            KeyStroke.getKeyStroke(KeyEvent.VK_E, 0)
    ),

    CopySteps(
            KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)
    ),

    CopyPreConditions(
            KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)
    ),

    CopyTestData(
            KeyStroke.getKeyStroke(KeyEvent.VK_T, 0)
    ),

    CopyPriority(
            KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)
    ),

    CopyModule(
            KeyStroke.getKeyStroke(KeyEvent.VK_M, 0)
    ),

    CopyGroup(
            KeyStroke.getKeyStroke(KeyEvent.VK_G, 0)
    ),

    CopyStatus(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0)
    ),

    CopyReference(
            KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)
    ),

    CopyFqcn(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)
    ),

    CopyUuid(
            KeyStroke.getKeyStroke(KeyEvent.VK_U, 0)
    ),

    CopyPath(
            KeyStroke.getKeyStroke(KeyEvent.VK_H, 0)
    ),

    TabNext(
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
    ),

    TabPrevious(
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
    ),

    ArrowDown(
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)
    ),

    ArrowUp(
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
    ),

    AutoComplete(
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK)
    ),

    CreateTestCaseDescription(
            KeyStroke.getKeyStroke(KeyEvent.VK_D, menuMask())
    ),

    CreateTestCaseExpectedResult(
            KeyStroke.getKeyStroke(KeyEvent.VK_E, menuMask())
    ),

    CreateTestCaseModule(
            KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
    ),

    CreateTestCaseSteps(
            KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask())
    ),

    CreateTestCaseGroup(
            KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK))
    ),

    CreateTestCasePriority(
            KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK))
    ),

    CreateTestCaseTestData(
            KeyStroke.getKeyStroke(KeyEvent.VK_T, menuMask())
    ),

    CreateTestCasePreConditions(
            KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | macAdds(InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
    ),

    CreateTestCaseReference(
            KeyStroke.getKeyStroke(KeyEvent.VK_R, menuMask())
    ),

    UpdateTestCaseDescription(
            KeyStroke.getKeyStroke(KeyEvent.VK_D, 0)
    ),

    UpdateTestCaseExpectedResult(
            KeyStroke.getKeyStroke(KeyEvent.VK_E, 0)
    ),

    UpdateTestCaseModule(
            KeyStroke.getKeyStroke(KeyEvent.VK_M, 0)
    ),

    UpdateTestCaseTestData(
            KeyStroke.getKeyStroke(KeyEvent.VK_T, 0)
    ),

    UpdateTestCasePreConditions(
            KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)
    ),

    UpdateTestCaseSteps(
            KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)
    ),

    UpdateTestCasePriority(
            KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)
    ),

    UpdateTestCaseGroup(
            KeyStroke.getKeyStroke(KeyEvent.VK_G, 0)
    ),

    UpdateTestCaseOrder(
            KeyStroke.getKeyStroke(KeyEvent.VK_O, 0)
    ),

    UpdateTestCaseReference(
            KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)
    ),

    UpdateTestCaseStatus(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0)
    );

    public static final @NotNull KeyStroke NO_KEY = EMPTY.getKey();
    private final @NotNull ImmutableList<KeyStroke> keys;

    Shortcuts(final @NotNull KeyStroke key, final @NotNull KeyStroke @NotNull ... alternatives) {
        this.keys = ImmutableList.<KeyStroke>builder().add(key).add(alternatives).build();
    }

    public @NotNull KeyStroke getKey() {
        return keys.getFirst();
    }

    @MagicConstant(flagsFromClass = InputEvent.class)
    public static int menuMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (final HeadlessException ex) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    @MagicConstant(flagsFromClass = InputEvent.class)
    private static int macAdds(@MagicConstant(flagsFromClass = InputEvent.class) final int modifiers) {
        return SystemInfo.isMac ? modifiers : 0;
    }

    public static @NotNull CustomShortcutSet customShortcut(final @NotNull KeyStroke key) {
        return new CustomShortcutSet(key);
    }

    public static @NotNull String shortcutText(final @NotNull KeyStroke key) {
        return isNoKey(key) ? "" : KeymapUtil.getKeystrokeText(key);
    }

    public static boolean isNoKey(final @NotNull KeyStroke key) {
        return NO_KEY.equals(key);
    }

    public static boolean matches(final @NotNull KeyEvent e, final @NotNull KeyStroke key) {
        return key.equals(KeyStroke.getKeyStrokeForEvent(e));
    }

    public @NotNull CustomShortcutSet getCustomShortcut() {
        return new CustomShortcutSet(keys.stream().map(Shortcuts::customShortcut).flatMap(set -> Arrays.stream(set.getShortcuts())).toArray(Shortcut[]::new));
    }

    public @NotNull String getShortcutText() {
        return shortcutText(getKey());
    }

    public boolean matches(final @NotNull KeyEvent e) {
        return keys.stream().anyMatch(stroke -> matches(e, stroke));
    }

    // Rule-INTERNAL-054
    public boolean is(final @NotNull ShortcutSet shortcutSet) {
        return Arrays.equals(shortcutSet.getShortcuts(), getCustomShortcut().getShortcuts());
    }
}
