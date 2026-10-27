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

package org.testin.editor.toolbar;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.UiInterceptors;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.editor.ShownFields;
import org.testin.testcase.TestSetEditorFields;
import org.testin.view.Drawn;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FieldsPopupIdeTest extends AbstractIdeTest {

    private @NotNull String chosenBefore = "";

    private static int indexOf(final @NotNull CheckBoxList<TestSetEditorFields> fields, final @NotNull TestSetEditorFields field) {
        for (int i = 0; i < fields.getModel().getSize(); i++)
            if (fields.getItemAt(i) == field) return i;
        throw new AssertionError(field.getName() + " is not on the list");
    }

    private static void drawn(final @NotNull CheckBoxList<TestSetEditorFields> fields) {
        fields.setSize(300, 800);
        for (int i = 0; i < fields.getModel().getSize(); i++)
            fields.getCellRenderer().getListCellRendererComponent(fields, fields.getModel().getElementAt(i), i, false, false);
    }

    private static void spaceOn(final @NotNull CheckBoxList<TestSetEditorFields> fields, final @NotNull TestSetEditorFields field) {
        fields.setSelectedIndex(indexOf(fields, field));
        final @NotNull KeyEvent space = new KeyEvent(fields, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, ' ');
        for (final KeyListener listener : fields.getKeyListeners()) listener.keyTyped(space);
    }

    @Override
    protected void setUp() {
        try {
            super.setUp();
            chosenBefore = Objects.toString(PropertiesComponent.getInstance().getValue(ShownFields.IN_TEST_SETS), "");
            PropertiesComponent.getInstance().unsetValue(ShownFields.IN_TEST_SETS);
        } catch (final Exception ex) {
            throw new AssertionError("Could not set up " + getName(), ex);
        }
    }

    @Override
    protected void tearDown() {
        try {
            if (chosenBefore.isEmpty()) PropertiesComponent.getInstance().unsetValue(ShownFields.IN_TEST_SETS);
            else PropertiesComponent.getInstance().setValue(ShownFields.IN_TEST_SETS, chosenBefore);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("Could not tear down " + getName(), ex);
        }
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-024
    public void testPressingFieldsOpensTheListOfFieldsUnderIt() {
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
        final @NotNull TestSetFieldsPopupButton button = new TestSetFieldsPopupButton(() -> {
        });

        button.doClick();

        assertEquals("pressing Fields did not open one list", 1, shown.size());
        try {
            assertEquals("the list opened does not hold every field the button offers", button.fieldList(() -> {
            }).getItemsCount(), Drawn.first(shown.getFirst().getContent(), CheckBoxList.class).getItemsCount());
        } finally {
            Disposer.dispose(shown.getFirst());
        }
    }

    // Rule-EDITOR-PANEL-023
    public void testDescriptionIsAlwaysShownOrderCanBeSwitchedOffAndUuidOn() {
        final @NotNull TestSetFieldsPopupButton button = new TestSetFieldsPopupButton(() -> {
        });
        final @NotNull CheckBoxList<TestSetEditorFields> fields = button.fieldList(() -> {
        });
        drawn(fields);

        assertFalse("UUID is shown before anyone switched it on", button.getSelectedDetails().contains(TestSetEditorFields.UUID));
        assertTrue("Order is not on to start with", button.getSelectedDetails().contains(TestSetEditorFields.ORDER));

        spaceOn(fields, TestSetEditorFields.ORDER);
        spaceOn(fields, TestSetEditorFields.DESCRIPTION);
        spaceOn(fields, TestSetEditorFields.UUID);

        assertFalse("Order could not be switched off", button.getSelectedDetails().contains(TestSetEditorFields.ORDER));
        assertTrue("Description could be switched off", fields.isItemSelected(TestSetEditorFields.DESCRIPTION) && button.getSelectedDetails().contains(TestSetEditorFields.DESCRIPTION));
        assertTrue("UUID could not be switched on", button.getSelectedDetails().contains(TestSetEditorFields.UUID));

        ShownFields.write(ShownFields.IN_TEST_SETS, EnumSet.of(TestSetEditorFields.UUID));
        assertTrue("a remembered choice without Description hid it", ShownFields.read(ShownFields.IN_TEST_SETS, TestSetEditorFields.class).contains(TestSetEditorFields.DESCRIPTION));
        assertFalse("a remembered choice without Order brought it back", ShownFields.read(ShownFields.IN_TEST_SETS, TestSetEditorFields.class).contains(TestSetEditorFields.ORDER));
    }

    // Rule-EDITOR-PANEL-272
    public void testTheGridNumberIsNotAFieldToChoose() {
        final @NotNull CheckBoxList<TestSetEditorFields> fields = new TestSetFieldsPopupButton(() -> {
        }).fieldList(() -> {
        });

        for (int i = 0; i < fields.getItemsCount(); i++) {
            assertNotSame("Choose Fields lists the grid's # column", TestSetEditorFields.SEQUENCE, fields.getItemAt(i));
        }
        assertTrue("a remembered choice could hide the grid's # column", ShownFields.read(ShownFields.IN_TEST_SETS, TestSetEditorFields.class).contains(TestSetEditorFields.SEQUENCE));
    }

    // Rule-EDITOR-PANEL-024
    public void testABurstOfTicksCostsOneRedraw() {
        final @NotNull AtomicInteger redraws = new AtomicInteger();
        final @NotNull TestSetFieldsPopupButton button = new TestSetFieldsPopupButton(redraws::incrementAndGet);
        final @NotNull CheckBoxList<TestSetEditorFields> fields = button.fieldList(redraws::incrementAndGet);
        drawn(fields);

        spaceOn(fields, TestSetEditorFields.STEPS);
        spaceOn(fields, TestSetEditorFields.MODULE);
        spaceOn(fields, TestSetEditorFields.TEST_DATA);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the ticks were not taken", button.getSelectedDetails().containsAll(EnumSet.of(TestSetEditorFields.STEPS, TestSetEditorFields.MODULE, TestSetEditorFields.TEST_DATA)));
        assertEquals("a burst of three ticks did not cost exactly one redraw", 1, redraws.get());

        spaceOn(fields, TestSetEditorFields.REFERENCE);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("a tick after the burst was drawn did not redraw", 2, redraws.get());
    }
}
