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

package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionWrapper;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Gestures;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.clipboard.CutTestCaseAction;
import org.testin.clipboard.PasteTestCaseAction;
import org.testin.editor.grid.GridKeys;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.GridViewBtn;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.testcase.RemoveTestCaseAction;
import org.testin.undo.UndoAction;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class KeyboardMenuIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull List<String> THE_SEVEN = List.of("Copy Test Case Value", "Copy Test Case", "Cut Test Case", "Paste Test Case", "Delete Test Case", "Undo", "Redo");

    private static @NotNull List<AnAction> childrenOf(final @NotNull DefaultActionGroup group) {
        return Arrays.stream(group.getChildren(ActionManager.getInstance())).filter(action -> !(action instanceof Separator)).toList();
    }

    private static @NotNull String nameOf(final @NotNull AnAction action) {
        return Objects.requireNonNullElse(action.getTemplatePresentation().getText(), "");
    }

    private static @NotNull DefaultActionGroup actionsEntryOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        final @NotNull List<DefaultActionGroup> groups = childrenOf(editor.contextMenu).stream()
                .filter(DefaultActionGroup.class::isInstance).map(DefaultActionGroup.class::cast)
                .filter(group -> nameOf(group).equals(Bundle.message("menu.actions"))).toList();
        assertEquals("the menu does not hold one Actions entry", 1, groups.size());
        return groups.getFirst();
    }

    private static @NotNull AnAction unwrapped(final @NotNull AnAction action) {
        return action instanceof final AnActionWrapper wrapper ? wrapper.getDelegate() : action;
    }

    private static @NotNull Set<KeyStroke> keysOf(final @NotNull AnAction action) {
        return Arrays.stream(action.getShortcutSet().getShortcuts()).filter(KeyboardShortcut.class::isInstance)
                .map(shortcut -> ((KeyboardShortcut) shortcut).getFirstKeyStroke()).collect(Collectors.toSet());
    }

    private static @NotNull Set<KeyStroke> keysBoundTo(final @NotNull JComponent on) {
        return ActionUtil.getActions(on).stream().flatMap(action -> keysOf(action).stream()).collect(Collectors.toSet());
    }

    private static @NotNull Set<KeyStroke> keysOfTheKeymap(final @NotNull AnAction entry) {
        final @NotNull String id = Objects.requireNonNullElse(ActionManager.getInstance().getId(unwrapped(entry)), "");
        return id.isEmpty() ? Set.of() : keysOf(ActionManager.getInstance().getAction(id));
    }

    private static @NotNull JBTable theGridOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.getToolBar().getToolbarItem(GridViewBtn.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return (JBTable) editor.getPreferredFocusedComponent();
    }

    private static void assertTheSevenAreUnderActionsAndTheirKeysStillWork(final @NotNull String which, final @NotNull AbstractTestinEditor<?, ?> editor) {
        final @NotNull List<AnAction> topLevel = childrenOf(editor.contextMenu);
        final @NotNull List<AnAction> underActions = childrenOf(actionsEntryOf(editor));

        assertEquals("the " + which + "'s Actions entry", THE_SEVEN, underActions.stream().map(KeyboardMenuIdeTest::nameOf).toList());
        for (final AnAction entry : topLevel) {
            assertFalse("the " + which + " menu holds " + nameOf(entry) + " at its top level", THE_SEVEN.contains(nameOf(entry)));
        }

        final @NotNull Set<KeyStroke> onTheCards = keysBoundTo(editor.getList());
        final @NotNull JBTable grid = theGridOf(editor);
        final @NotNull Set<KeyStroke> onTheGrid = keysBoundTo(grid);
        for (final AnAction entry : underActions) {
            for (final KeyStroke key : keysOf(entry)) {
                assertTrue("in the " + which + " the key " + key + " of " + nameOf(entry) + " does not reach the cards", onTheCards.contains(key) || keysOfTheKeymap(entry).contains(key));
                assertTrue("in the " + which + " the key " + key + " of " + nameOf(entry) + " does not reach the grid", onTheGrid.contains(key) || GridKeys.keptFromMenus().contains(key));
            }
        }
    }

    private static @NotNull String said(final @NotNull Presentation shown) {
        return Objects.requireNonNullElse(shown.getDescription(), "");
    }

    private @NotNull TestSetEditor aTestSetEditor() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode ts = EditorFixtures.testSet(getProject(), tp, "Checkout");
        EditorFixtures.testCases(getProject(), ts, 2);
        return EditorFixtures.openTestSetEditor(getProject(), ts, getTestRootDisposable());
    }

    private @NotNull TestRunEditor aTestRunEditor() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode ts = EditorFixtures.testSet(getProject(), tp, "Payments");
        final @NotNull List<TestCaseDto> inTheTestRun = EditorFixtures.testCases(getProject(), ts, 2);
        final @NotNull TestRunNode tr = EditorFixtures.testRun(getProject(), tp, inTheTestRun.stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
    }

    private @NotNull Presentation updated(final @NotNull AbstractTestinEditor<?, ?> editor, final @NotNull AnAction entry) {
        return Gestures.updated(getProject(), entry, editor.getList());
    }

    // Rule-EDITOR-PANEL-213
    public void testTheClipboardAndHistoryEntriesAreUnderActionsAndTheirKeysStillWork() {
        final @NotNull TestSetEditor testSetEditor = aTestSetEditor();
        final @NotNull TestRunEditor testRunEditor = aTestRunEditor();
        try {
            assertTheSevenAreUnderActionsAndTheirKeysStillWork("test set editor", testSetEditor);
            assertTheSevenAreUnderActionsAndTheirKeysStillWork("test run editor", testRunEditor);
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-214
    public void testATestRunEditorShowsTheSameSevenAndRefusesFourWithTheirReason() {
        final @NotNull TestSetEditor testSetEditor = aTestSetEditor();
        final @NotNull TestRunEditor testRunEditor = aTestRunEditor();
        final @NotNull Map<Class<?>, String> refusedInATestRun = Map.of(
                CopyTestCaseAction.class, Bundle.message("copy.test.case.disabled.description"),
                CutTestCaseAction.class, Bundle.message("cut.test.case.disabled.description"),
                PasteTestCaseAction.class, Bundle.message("paste.test.case.disabled.description"),
                RemoveTestCaseAction.class, Bundle.message("remove.test.case.disabled.description"));
        try {
            testSetEditor.getList().setSelectedIndex(0);
            testRunEditor.getList().setSelectedIndex(0);
            CopyPasteManager.getInstance().setContents(new StringSelection("plain words"));

            assertEquals(THE_SEVEN, childrenOf(actionsEntryOf(testRunEditor)).stream().map(KeyboardMenuIdeTest::nameOf).toList());
            for (final AnAction entry : childrenOf(actionsEntryOf(testRunEditor))) {
                final @NotNull Class<?> kind = unwrapped(entry).getClass();
                if (!refusedInATestRun.containsKey(kind)) continue;

                final @NotNull Presentation inTheTestRun = updated(testRunEditor, entry);
                assertTrue(nameOf(entry) + " is left out of the test run editor's menu", inTheTestRun.isVisible());
                assertFalse(nameOf(entry) + " works in a test run editor", inTheTestRun.isEnabled());
                assertEquals(nameOf(entry) + " does not say why it is gray in a test run", refusedInATestRun.get(kind), said(inTheTestRun));
            }

            for (final AnAction entry : childrenOf(actionsEntryOf(testSetEditor))) {
                final @NotNull Class<?> kind = unwrapped(entry).getClass();
                if (!refusedInATestRun.containsKey(kind) || kind == PasteTestCaseAction.class) continue;

                assertTrue(nameOf(entry) + " is gray in a test set editor with a test case selected", updated(testSetEditor, entry).isEnabled());
            }
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-230
    public void testAnEntryThatCannotWorkIsGrayAndSaysWhy() {
        final @NotNull TestSetEditor testSetEditor = aTestSetEditor();
        final @NotNull TestRunEditor testRunEditor = aTestRunEditor();
        try {
            CopyPasteManager.getInstance().setContents(new StringSelection("plain words"));
            testSetEditor.getList().clearSelection();
            testRunEditor.getList().clearSelection();

            final @NotNull List<String> silent = new ArrayList<>();
            for (final AbstractTestinEditor<?, ?> editor : List.<AbstractTestinEditor<?, ?>>of(testSetEditor, testRunEditor)) {
                final @NotNull List<AnAction> entries = new ArrayList<>(childrenOf(editor.contextMenu));
                entries.addAll(childrenOf(actionsEntryOf(editor)));
                for (final AnAction entry : entries) {
                    if (entry instanceof DefaultActionGroup || unwrapped(entry) instanceof UndoAction) continue;

                    final @NotNull Presentation shown = updated(editor, entry);
                    if (shown.isEnabled()) continue;

                    final @NotNull String template = Objects.requireNonNullElse(entry.getTemplatePresentation().getDescription(), "");
                    if (said(shown).isBlank() || said(shown).equals(template))
                        silent.add(nameOf(entry) + " in the " + editor.getClass().getSimpleName());
                }
            }
            assertEquals("these entries are gray with nothing selected and do not say why", List.of(), silent);

            final @NotNull Presentation paste = updated(testSetEditor, childrenOf(actionsEntryOf(testSetEditor)).get(3));
            assertFalse("Paste works with no test cases on the clipboard", paste.isEnabled());
            assertEquals("Paste does not give the clipboard its own reason", Bundle.message("paste.test.case.nothing.description"), said(paste));
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }
}
