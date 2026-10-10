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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.List;

public class ShortcutStripIdeTest extends BasePlatformTestCase {


    private static @NotNull StatusBarItem @NotNull [] sixKeys() {
        return new StatusBarItem[]{
                StatusBarShortcut.build(Shortcuts.Enter, "Confirm", EmptyRunnable.getInstance()),
                StatusBarShortcut.select(),
                StatusBarShortcut.navigate(),
                StatusBarShortcut.build(Shortcuts.FocusSearch, "Search", EmptyRunnable.getInstance()),
                StatusBarShortcut.corrections(),
                StatusBarShortcut.cancel(EmptyRunnable.getInstance())};
    }

    private static void laidOut(final @NotNull Container container) {
        container.doLayout();
        for (final Component child : container.getComponents()) {
            if (child instanceof final Container inner) laidOut(inner);
        }
    }

    private static @NotNull JComponent stripAt(final int width) {
        final @NotNull JComponent strip = new StatusBarBase(sixKeys()).getPanel();
        strip.setSize(width, strip.getPreferredSize().height);
        laidOut(strip);
        return strip;
    }

    private static @NotNull List<JLabel> wordsOf(final @NotNull JComponent strip) {
        return Drawn.components(strip).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).filter(label -> !Drawn.text(label).isEmpty()).toList();
    }

    private static int xOf(final @NotNull Component component, final @NotNull Component within) {
        return SwingUtilities.convertPoint(component.getParent(), component.getLocation(), within).x;
    }

    private static int middleOf(final @NotNull Component component, final @NotNull Component within) {
        return SwingUtilities.convertPoint(component.getParent(), component.getLocation(), within).y + component.getHeight() / 2;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-078
    public void testTheStripIsOneRowAndANarrowDialogShortensItRatherThanFoldingIt() {
        final int oneRow = stripAt(2_000).getPreferredSize().height;
        final @NotNull JComponent narrow = stripAt(160);

        assertEquals("a narrow dialog grew its strip a line taller", oneRow, narrow.getPreferredSize().height);
        final int middle = middleOf(wordsOf(narrow).getFirst(), narrow);
        for (final JLabel word : wordsOf(narrow)) {
            assertEquals("the strip folded '" + Drawn.text(word) + "' onto a second line", middle, middleOf(word, narrow));
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-079
    public void testASurfaceHasOneShortcutStrip() {
        final @NotNull Form form = new Form(getProject());
        form.show();
        try {
            final long strips = Drawn.components(form.root()).stream().filter(component -> component.getClass().getEnclosingClass() == StatusBarBase.class).count();
            assertEquals("a dialog carries more or fewer than one shortcut strip", 1, strips);
        } finally {
            form.closeCancel();
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-104
    public void testTheKeysNeverMakeTheDialogWiderAndAKeyWithNoRoomIsNotDrawn() {
        final @NotNull JTextField field = new JTextField(12);
        final @NotNull JPanel dialog = new JPanel(new BorderLayout());
        dialog.add(field, BorderLayout.CENTER);
        dialog.add(new StatusBarBase(sixKeys()).getPanel(), BorderLayout.SOUTH);

        assertEquals("the keys made the dialog wider than what the tester fills in", field.getPreferredSize().width, dialog.getPreferredSize().width);

        final @NotNull JComponent narrow = stripAt(220);
        for (final JLabel word : wordsOf(narrow)) {
            final int left = xOf(word, narrow);
            final boolean whole = left + word.getWidth() <= narrow.getWidth();
            final boolean gone = left >= narrow.getWidth();
            assertTrue("the key '" + Drawn.text(word) + "' is cut in half instead of not being drawn", whole || gone);
        }

        final @NotNull JComponent wide = stripAt(2_000);
        for (final JLabel word : wordsOf(wide)) {
            assertTrue("widening the dialog did not show '" + Drawn.text(word) + "'", xOf(word, wide) + word.getWidth() <= wide.getWidth());
        }
    }

    private static final class Form extends AbstractFrameworkDialog {
        private Form(final @NotNull Project p) {
            super(p);
            title = "Every field";
            components = List.of(ComponentDialogBase.textField().caption("Name").placeholder("set name..").build());
            shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Enter, "Save", this::submit), StatusBarShortcut.cancel(this::closeCancel));
        }

        @Override
        protected void submit() {
        }
    }
}
