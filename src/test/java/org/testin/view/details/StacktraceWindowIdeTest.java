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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.framework.ShownDialog;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Locale;

public class StacktraceWindowIdeTest extends BasePlatformTestCase {

    private static final @NotNull String STACKTRACE = """
            java.lang.AssertionError: expected [true]
                at org.testin.demo.LoginTest.valid(LoginTest:41)""";

    private static void layOut(final @NotNull Container container) {
        container.doLayout();
        for (final Component child : container.getComponents()) {
            if (child instanceof final Container inner) layOut(inner);
        }
    }

    private static int topOf(final @NotNull Component part, final @NotNull JComponent window) {
        return SwingUtilities.convertPoint(part, 0, 0, window).y;
    }

    private static @NotNull JBLabel labelHolding(final @NotNull List<Component> drawn, final @NotNull String text) {
        return drawn.stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast)
                .filter(label -> Drawn.text(label).contains(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the window does not hold \"" + text + "\""));
    }

    private static @NotNull JTextComponent theBox(final @NotNull List<Component> drawn) {
        final @NotNull List<JTextComponent> boxes = drawn.stream().filter(JTextComponent.class::isInstance).map(JTextComponent.class::cast).toList();
        assertEquals("the window holds more than the one box the error sits in", 1, boxes.size());
        return boxes.getFirst();
    }

    private @NotNull JComponent shownWindow() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().description("Log in with a valid user").expectedResult("The session stays until the tester signs out").build();
        assertTrue("the Stacktrace window did not open", new StacktraceDialog(getProject(), tc, "The session was dropped", STACKTRACE).show());

        final @NotNull JComponent window = ShownDialog.content(getProject(), StacktraceDialog.class);
        window.setSize(900, 700);
        layOut(window);
        return window;
    }

    @Override
    protected void tearDown() {
        try {
            ShownDialog.close(getProject(), StacktraceDialog.class);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("Could not tear down " + getName() + ": " + ex.getMessage(), ex);
        }
    }

    // Rule-VIEW-PANEL-035
    public void testTheWindowHoldsTheTestCaseAndWhatTheTesterWroteAboveTheError() {
        final @NotNull JComponent window = shownWindow();
        final @NotNull List<Component> drawn = Drawn.components(window);
        final @NotNull JTextComponent box = theBox(drawn);

        assertEquals("the box holds something other than the error", STACKTRACE, box.getText());
        for (final String above : List.of("Log in with a valid user", "The session stays until the tester signs out", "The session was dropped")) {
            final @NotNull JBLabel label = labelHolding(drawn, above);
            assertTrue("\"" + above + "\" is not drawn above the error", topOf(label, window) < topOf(box, window));
            assertFalse("\"" + above + "\" shares the error's box", SwingUtilities.isDescendingFrom(label, box));
        }
    }

    // Rule-VIEW-PANEL-036
    public void testOnlyTheErrorSelectsAndNothingCanBeTypedInto() {
        final @NotNull JComponent window = shownWindow();
        final @NotNull List<Component> drawn = Drawn.components(window);
        final @NotNull JTextComponent box = theBox(drawn);

        assertFalse("the error can be typed into", box.isEditable());
        assertTrue("the error cannot be selected", box.isFocusable() && box.isEnabled());
        box.selectAll();
        assertEquals("selecting the error did not take all of it", STACKTRACE, box.getSelectedText());

        assertSame("the test case does not come after its icon", CreateTestCaseFields.DESCRIPTION.getIcon(), labelHolding(drawn, "Log in with a valid user").getIcon());
        assertSame("the expected result does not come after its icon", CreateTestCaseFields.EXPECTED_RESULT.getIcon(), labelHolding(drawn, "The session stays until the tester signs out").getIcon());
        final @NotNull JBLabel caption = labelHolding(drawn, TestRunEditorAttributes.ACTUAL_RESULT.getName().toUpperCase(Locale.ROOT));
        final @NotNull JBLabel resultLabel = labelHolding(drawn, "The session was dropped");
        assertSame("the actual result's caption does not name it", resultLabel, caption.getLabelFor());
        assertTrue("the actual result does not come after its caption", topOf(caption, window) < topOf(resultLabel, window));
    }
}
