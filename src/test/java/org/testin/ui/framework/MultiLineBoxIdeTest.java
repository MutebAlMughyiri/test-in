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

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Fonts;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiLineBoxIdeTest extends BasePlatformTestCase {

    private final @NotNull List<EditorTextField> realized = new ArrayList<>();

    private static @NotNull String lines(final int count) {
        final @NotNull StringBuilder text = new StringBuilder("Open the login page");
        for (int line = 1; line < count; line++) text.append("\nStep ").append(line);
        return text.toString();
    }

    @Override
    protected void tearDown() {
        try {
            realized.forEach(EditorTextField::removeNotify);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("could not tear down " + getName(), ex);
        }
    }

    private @NotNull MultiLineField aBox() {
        return DialogPart.multiLineField(getProject(), "Steps", "write the steps..", "").getComponent();
    }

    private @NotNull Editor editorOf(final @NotNull MultiLineField box) {
        final @NotNull EditorTextField field = (EditorTextField) box.getFocusComponent();
        new JPanel().add(box.getPanel());
        field.addNotify();
        realized.add(field);
        return Optional.ofNullable(field.getEditor(true)).orElseThrow(() -> new AssertionError("the box of many lines has no editor"));
    }

    // Rule-INTERNAL-134
    public void testABoxNeverOpensIntelliJsFindOrReplace() {
        assertTrue("a dialog field opens IntelliJ's Find or Replace bar", Optional.ofNullable(editorOf(aBox()).getUserData(IncrementalFindAction.SEARCH_DISABLED)).orElse(false));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-097
    public void testABoxOfManyLinesGrowsAsLinesAreAddedAndWhatDrawsItGrowsWithIt() {
        final @NotNull MultiLineField box = aBox();
        final @NotNull Host host = new Host();
        box.hostedBy(host, () -> {
        });
        editorOf(box);
        final int oneLine = box.getFocusComponent().getPreferredSize().height;

        box.setText(lines(3));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the box did not grow as lines were added", box.getFocusComponent().getPreferredSize().height > oneLine);
        assertTrue("what draws the box was not asked to grow with it", host.refits.get() > 0);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-097
    public void testEnterBelongsToWhoeverDrawsTheBoxCtrlEnterAddsALineAndTabLeavesIt() {
        final @NotNull MultiLineField box = aBox();
        final @NotNull Host host = new Host();
        final @NotNull AtomicInteger saved = new AtomicInteger();
        box.hostedBy(host, saved::incrementAndGet);
        final @NotNull Editor editor = editorOf(box);

        host.bound(box.getFocusComponent(), Shortcuts.Enter).orElseThrow(() -> new AssertionError("the box did not ask what draws it to bind Enter")).run();
        assertEquals("Enter did not do what the dialog drawing the box does with it", 1, saved.get());

        box.setText("Open the login page");
        editor.getCaretModel().moveToOffset(editor.getDocument().getTextLength());
        host.bound(editor.getContentComponent(), Shortcuts.InsertNewLine).orElseThrow(() -> new AssertionError("Ctrl+Enter is not bound in the box")).run();
        assertEquals("Ctrl+Enter did not add a line", "Open the login page\n", box.getText());

        assertTrue("Tab does not leave the box", editor.getContentComponent().getFocusTraversalKeysEnabled());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-097
    public void testEveryBoxOfManyLinesIsTheSameBoxInTheFieldsFont() {
        final @NotNull MultiLineField steps = aBox();
        final @NotNull MultiLineField expected = DialogPart.multiLineField(getProject(), "Expected result", "write what should happen..", "").getComponent();

        assertEquals("a box of many lines is not in the field font", Fonts.field(), steps.getFocusComponent().getFont());
        assertEquals("two boxes of many lines are not the same box", steps.getFocusComponent().getFont(), expected.getFocusComponent().getFont());
    }

    // UC-INTERNAL-007, Rule-INTERNAL-102
    public void testABoxOfManyLinesShowsSixAndScrollsPastThem() {
        final @NotNull MultiLineField box = aBox();
        editorOf(box);
        final int lineHeight = box.getFocusComponent().getFontMetrics(Fonts.field()).getHeight();

        box.setText(lines(20));

        assertEquals("a box of twenty lines does not stop at six", 6 * lineHeight, box.getFocusComponent().getPreferredSize().height);
    }

    // Rule-INTERNAL-136
    public void testAGrowingBoxStartsAtThreeLinesAndShowsAWholePasteUnderAScrollbarOverTheText() {
        final @NotNull MultiLineField box = DialogPart.multiLineField(getProject(), "", "Paste the request body", 3, MultiLineField.NO_LIMIT, PlainTextLanguage.INSTANCE, false).getComponent();
        final @NotNull Editor editor = editorOf(box);
        final int line = box.getFocusComponent().getFontMetrics(Fonts.field()).getHeight();

        assertTrue("an empty growing box is shorter than three lines", box.getFocusComponent().getPreferredSize().height >= 3 * line);

        box.setText(lines(20));
        assertTrue("a growing box stopped before the whole paste showed", box.getFocusComponent().getPreferredSize().height >= 20 * line);
        assertTrue("a growing box's scrollbar takes space beside the text", ((EditorEx) editor).getScrollPane() instanceof final JBScrollPane pane && pane.isOverlappingScrollBar());
    }

    // Rule-INTERNAL-136, Rule-INTERNAL-137
    public void testAGrowingBoxStopsAtTheHeightItIsCappedAt() {
        final @NotNull MultiLineField box = DialogPart.multiLineField(getProject(), "", "", 3, MultiLineField.NO_LIMIT, PlainTextLanguage.INSTANCE, false).getComponent();
        editorOf(box);
        final int line = box.getFocusComponent().getFontMetrics(Fonts.field()).getHeight();

        box.capHeight(() -> 5 * line);
        box.setText(lines(20));

        assertEquals("a growing box went past its cap", 5 * line, box.getFocusComponent().getPreferredSize().height);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-103
    public void testABoxThatSpellChecksShowsNoBulbIconOrButton() {
        final @NotNull MultiLineField box = aBox();
        final @NotNull Editor editor = editorOf(box);

        assertFalse("the box offers corrections with a bulb", editor.getSettings().isShowIntentionBulb());
        final @NotNull List<Component> extras = Drawn.components(box.getPanel()).stream().filter(component -> component instanceof AbstractButton || (component instanceof final JLabel label && label.getIcon() != null)).toList();
        assertEquals("the box draws an icon or a button beside what is typed", List.of(), extras);
    }

    private static final class Host implements DialogHost {
        private final @NotNull JComponent root = new JPanel();

        private final @NotNull AtomicInteger refits = new AtomicInteger();

        private final @NotNull List<Binding> bindings = new ArrayList<>();

        @Override
        public @NotNull JComponent root() {
            return root;
        }

        @Override
        public void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull Runnable action) {
            bindings.add(new Binding(component, shortcutSet, action));
        }

        @Override
        public void refit() {
            refits.incrementAndGet();
        }

        private @NotNull Optional<Runnable> bound(final @NotNull Component on, final @NotNull Shortcuts key) {
            return bindings.stream().filter(binding -> binding.on().equals(on) && key.is(binding.keys())).map(Binding::action).findFirst();
        }
    }

    private record Binding(@NotNull JComponent on, @NotNull CustomShortcutSet keys, @NotNull Runnable action) {
    }
}
