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

package org.testin.testcase.update.bulk;

import com.intellij.lang.Language;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diff.DiffColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.Json;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.KeyStroke;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class BulkEditIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String MODULE = "\"module\": \"";

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static int startOfValue(final @NotNull Document document, final int index) {
        int at = -1;
        for (int i = 0; i <= index; i++) at = document.getText().indexOf(MODULE, at + 1);
        assertTrue("there is no value " + index, at >= 0);
        return at + MODULE.length();
    }

    private static int endOfValue(final @NotNull Document document, final int index) {
        return document.getText().indexOf('"', startOfValue(document, index));
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError(file + " could not be read", ex);
        }
    }

    private static boolean green(final @NotNull Color color) {
        return color.getGreen() > color.getRed() && color.getGreen() > color.getBlue();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ModuleBulkSectionDialog.class);
        ShownDialog.close(getProject(), StepsBulkSectionDialog.class);
        super.tearDown();
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private @NotNull List<TestCase> aTestSetWithModules(final @NotNull String... modules) {
        testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        final @NotNull List<TestCase> made = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            final @NotNull TestCase testCase = TestCase.builder().id(UUID.randomUUID()).description("Test case number " + (i + 1)).order(String.format("m%04d", i)).module(modules[i]).build();
            testCase.setParent(testSet);
            theTestCases().putTestCaseVerbatim(testSet.getPath(), testCase);
            made.add(theTestCases().findTestCase(testCase.getId()).orElseThrow());
        }
        return made;
    }

    private void bulkEditTheModules() {
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        editor.getList().setSelectionInterval(0, editor.getList().getModel().getSize() - 1);

        UpdateTestCaseAction.openField(getProject(), editor, UpdateTestCaseFields.MODULE);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull List<Editor> editors() {
        return editorsOf(ModuleBulkSectionDialog.class);
    }

    private @NotNull List<Editor> editorsOf(final @NotNull Class<?> kind) {
        return Drawn.components(ShownDialog.content(getProject(), kind)).stream()
                .filter(EditorComponentImpl.class::isInstance).map(component -> (Editor) ((EditorComponentImpl) component).getEditor()).toList();
    }

    private @NotNull Editor originals() {
        return editors().stream().filter(Editor::isViewer).findFirst().orElseThrow(() -> new AssertionError("the originals are not shown"));
    }

    private @NotNull Editor values() {
        return editors().stream().filter(editor -> !editor.isViewer()).findFirst().orElseThrow(() -> new AssertionError("there is nothing to type into"));
    }

    private void type(final int index, final @NotNull String value) {
        final @NotNull Document document = values().getDocument();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.replaceString(startOfValue(document, index), endOfValue(document, index), value));
    }

    private void save() {
        ShownDialog.press(getProject(), ModuleBulkSectionDialog.class, Shortcuts.Enter.getKey());
    }

    private @NotNull List<String> whatTheSaveSaid() {
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        save();
        Await.until("the save never said what it did", () -> !balloons.shown().isEmpty());
        return balloons.shown();
    }

    private @NotNull List<String> everyFile() {
        try (final Stream<Path> files = Files.list(testSet.getPath())) {
            return files.filter(Files::isRegularFile).sorted().map(file -> file + "@" + file.toFile().lastModified() + "=" + read(file)).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    private @NotNull String moduleOf(final @NotNull TestCase testCase) {
        return theTestCases().findTestCase(testCase.getId()).orElseThrow().getModule();
    }

    private @NotNull List<RangeHighlighter> greenIn(final @NotNull Editor editor) {
        return Stream.of(editor.getMarkupModel().getAllHighlighters()).filter(highlighter -> DiffColors.DIFF_INSERTED.equals(highlighter.getTextAttributesKey())).toList();
    }

    // Rule-EDITOR-PANEL-040, Rule-EDITOR-PANEL-045
    public void testTheValuesAreEditedInPlaceBesideTheOriginalsAndAnEmptyOneStillGetsALine() {
        final @NotNull List<TestCase> testCases = aTestSetWithModules("Payments", "", "Cart");
        bulkEditTheModules();

        assertTrue("the originals can be typed into", originals().isViewer() && !originals().getDocument().isWritable());
        for (final String module : List.of("Payments", "Cart")) {
            assertTrue("the originals do not hold " + module, originals().getDocument().getText().contains(MODULE + module + "\""));
            assertTrue("the values to edit do not hold " + module, values().getDocument().getText().contains(MODULE + module + "\""));
        }
        assertEquals("the test case with no module has no line of its own", startOfValue(values().getDocument(), 1), endOfValue(values().getDocument(), 1));

        type(1, "Basket");
        whatTheSaveSaid();

        assertEquals("the value typed on the empty line was never written", "Basket", moduleOf(testCases.get(1)));
    }

    // Rule-EDITOR-PANEL-041, Rule-EDITOR-PANEL-042
    public void testOnlyTheRowsTheTesterEditedAreWrittenAndTheyAreTrimmed() {
        final @NotNull List<TestCase> testCases = aTestSetWithModules("  Payments  ", "Cart");
        final @NotNull String untouched = everyFile().stream().filter(file -> file.contains(testCases.getFirst().getId().toString())).findFirst().orElseThrow();
        bulkEditTheModules();

        type(1, "  Basket  ");
        whatTheSaveSaid();

        assertEquals("the edited row was not written trimmed", "Basket", moduleOf(testCases.get(1)));
        assertEquals("a row the tester did not touch was trimmed", "  Payments  ", moduleOf(testCases.getFirst()));
        assertTrue("a row the tester did not touch had its file written", everyFile().contains(untouched));
    }

    // Rule-EDITOR-PANEL-043
    public void testAnEditedValueTakesAGreenBackground() {
        aTestSetWithModules("Payments", "Cart");
        bulkEditTheModules();
        assertTrue("an untouched value is marked as edited", greenIn(values()).isEmpty());

        type(1, "Basket");

        Await.until("the edited value never took a background", () -> !greenIn(values()).isEmpty());
        final @NotNull RangeHighlighter marked = greenIn(values()).getFirst();
        assertEquals("more than the edited value took a background", 1, greenIn(values()).size());
        assertEquals("the background is not on the edited value", startOfValue(values().getDocument(), 1), marked.getStartOffset());

        final @NotNull EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        final @NotNull Color background = Optional.ofNullable(scheme.getAttributes(DiffColors.DIFF_INSERTED).getBackgroundColor()).orElseThrow(() -> new AssertionError("an edited value takes no background color"));
        assertTrue("an edited value's background is not green: " + background, green(background));
        Optional.ofNullable(EditorColorsManager.getInstance().getScheme("Darcula")).map(dark -> dark.getAttributes(DiffColors.DIFF_INSERTED).getBackgroundColor())
                .ifPresent(dark -> assertTrue("an edited value's background is not green in the dark theme: " + dark, green(dark)));
    }

    // Rule-EDITOR-PANEL-046, Rule-EDITOR-PANEL-038, Rule-EDITOR-PANEL-008
    public void testTheWholeGestureIsOneUndoEntryAndOneMessageWithACount() {
        final @NotNull List<TestCase> testCases = aTestSetWithModules("Payments", "Cart", "Search");
        final @NotNull UndoScope scope = UndoScope.of(testSet.getPath());
        bulkEditTheModules();

        type(0, "Billing");
        type(1, "Basket");
        type(2, "Find");
        final @NotNull List<String> said = whatTheSaveSaid();

        assertTrue("the gesture never reached the undo history", undoHistories().canUndo(scope));
        assertEquals(Bundle.message("snapshot.undo.many", Bundle.message("snapshot.verb.update"), "3"), undoHistories().undoDescription(scope));
        assertEquals("the change to three test cases did not say so once, with a count", List.of(Done.counted(Done.UPDATED.getOutcome(), 3)), said);

        assertTrue("the gesture could not be taken back", undoHistories().undo(scope));
        assertEquals(List.of("Payments", "Cart", "Search"), testCases.stream().map(this::moduleOf).toList());
        assertFalse("one gesture made more than one undo entry", undoHistories().canUndo(scope));
    }

    // Rule-EDITOR-PANEL-292
    public void testBothSidesShowTheListInJsonColors() {
        aTestSetWithModules("Payments", "Cart");
        bulkEditTheModules();

        assertEquals("the bulk editor is not two editors", 2, editors().size());
        for (final Editor editor : editors()) {
            final @NotNull Language colored = editor.getHighlighter().createIterator(startOfValue(editor.getDocument(), 0)).getTokenType().getLanguage();
            assertEquals("a value is not colored as JSON", Json.language(), colored);
        }
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-293
    public void testTheAddKeyAddsAnEntryToABulkList() {
        aTestSetWithModules("Payments", "Cart");
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        editor.getList().setSelectionInterval(0, editor.getList().getModel().getSize() - 1);
        UpdateTestCaseAction.openField(getProject(), editor, UpdateTestCaseFields.STEPS);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull Document steps = editorsOf(StepsBulkSectionDialog.class).stream().filter(values -> !values.isViewer()).findFirst().orElseThrow().getDocument();
        final int lines = steps.getLineCount();

        for (final KeyStroke stroke : Shortcuts.AddItem.getKeys())
            ShownDialog.press(getProject(), StepsBulkSectionDialog.class, stroke);

        assertEquals("not every add key added an item to the steps", lines + Shortcuts.AddItem.getKeys().length, steps.getLineCount());
    }

    // Rule-EDITOR-PANEL-006
    public void testASaveThatWouldLeaveTheFilesAsTheyAreWritesNothingAndSaysNothing() {
        aTestSetWithModules("Payments", "Cart");
        final @NotNull List<String> before = everyFile();
        bulkEditTheModules();
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        type(0, "Payments");
        save();

        assertEquals("a save that changed nothing said something", List.of(), balloons.shown());
        assertEquals("a save that changed nothing wrote a file", before, everyFile());
        assertFalse("a save that changed nothing went on the undo history", undoHistories().canUndo(UndoScope.of(testSet.getPath())));
    }
}
