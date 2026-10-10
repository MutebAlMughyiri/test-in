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
package org.testin.view.details;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;
import org.testin.view.KeyPress;
import org.testin.view.PopupsBuilt;
import org.testin.view.ViewOnScreen;
import org.testin.view.ViewTab;

import javax.swing.KeyStroke;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FieldChange {

    static @NotNull KeyStroke f2() {
        return ((KeyboardShortcut) Declared.shortcutSet("Testin.UpdateTestCase").getShortcuts()[0]).getFirstKeyStroke();
    }

    static boolean pressF2(final @NotNull Project p, final @NotNull ViewOnScreen view) {
        return KeyPress.press(p, view.keyboardTarget(ViewTab.DETAILS), f2());
    }

    static @NotNull JBList<?> menuOf(final @NotNull PopupsBuilt.Built menu) {
        return Drawn.components(menu.getContent()).stream()
                .filter(JBList.class::isInstance)
                .map(JBList.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the popup is not a menu of fields"));
    }

    static void chooseTheDescription(final @NotNull PopupsBuilt.Built menu) {
        final @NotNull JBList<?> list = menuOf(menu);
        list.setSize(400, 800);
        final int row = Arrays.asList(UpdateTestCaseFields.values()).indexOf(UpdateTestCaseFields.DESCRIPTION);
        final @NotNull Rectangle cell = list.getCellBounds(row, row);
        final @NotNull MouseEvent click = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, cell.x + 2, cell.y + cell.height / 2, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : list.getMouseListeners()) listener.mouseClicked(click);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    static void typeAndSave(final @NotNull Project p, final @NotNull String now) {
        final @NotNull EditorTextField field = Drawn.components(ShownDialog.content(p, UpdateTestCaseDialog.class)).stream()
                .filter(EditorTextField.class::isInstance)
                .map(EditorTextField.class::cast)
                .filter(shown -> shown.getText().equals("Log in with a valid user"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the field's dialog does not hold \"" + "Log in with a valid user" + "\""));
        field.setText(now);

        if (!KeyPress.press(p, field, Shortcuts.Enter.getKey()))
            throw new AssertionError("Enter did not save the field's dialog");
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }
}
