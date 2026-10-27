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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diff.DiffColors;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Await;
import org.testin.view.Drawn;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BulkJsonEditorsIdeTest extends AbstractIdeTest {

    private static final @NotNull List<String> MODULES = List.of("Login", "Payments");

    private static final @NotNull String KEY = "\"module\": \"";

    private static final @NotNull String JSON = "[\n  {\n    " + KEY + "Login\"\n  },\n  {\n    " + KEY + "Payments\"\n  }\n]";

    private BulkJsonEditors editors;

    private static @NotNull List<int[]> theValues() {
        final @NotNull List<int[]> ranges = new ArrayList<>();
        for (int at = JSON.indexOf(KEY); at >= 0; at = JSON.indexOf(KEY, at + 1)) {
            final int start = at + KEY.length();
            ranges.add(new int[]{start, JSON.indexOf('"', start)});
        }
        return ranges;
    }

    private static int startOfTheFirstValue() {
        return theValues().getFirst()[0];
    }

    private static int endOf(final int index) {
        return theValues().get(index)[1];
    }

    private static @NotNull List<Integer> caretOffsets(final @NotNull Editor editor) {
        return editor.getCaretModel().getAllCarets().stream().map(Caret::getOffset).toList();
    }

    private static @NotNull List<RangeHighlighter> greenIn(final @NotNull Editor editor) {
        return Stream.of(editor.getMarkupModel().getAllHighlighters()).filter(highlighter -> DiffColors.DIFF_INSERTED.equals(highlighter.getTextAttributesKey())).toList();
    }

    private static void pressed(final @NotNull Editor editor, final int offset, final boolean holdingCtrl) {
        final @NotNull Point at = editor.offsetToXY(offset);
        editor.getContentComponent().dispatchEvent(new MouseEvent(editor.getContentComponent(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), holdingCtrl ? InputEvent.CTRL_DOWN_MASK | InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK, at.x + 1, at.y + 1, 1, false, MouseEvent.BUTTON1));
    }

    @Override
    protected void setUp() {
        super.setUp();
        editors = new BulkJsonEditors(getProject());
        editors.setContent(JSON, JSON, theValues());
        editors.setOriginalTextSource(MODULES::get);
    }

    @Override
    protected void tearDown() {
        editors.release();
        super.tearDown();
    }

    private @NotNull List<Editor> both() {
        return Drawn.components(editors.getPanel()).stream().filter(EditorComponentImpl.class::isInstance).map(component -> (Editor) ((EditorComponentImpl) component).getEditor()).toList();
    }

    private @NotNull Editor originals() {
        return both().stream().filter(Editor::isViewer).findFirst().orElseThrow(() -> new AssertionError("the originals are not shown"));
    }

    private @NotNull Editor values() {
        return both().stream().filter(editor -> !editor.isViewer()).findFirst().orElseThrow(() -> new AssertionError("there is nothing to type into"));
    }

    private void typedIntoTheFirstValue(final @NotNull String value) {
        final int start = startOfTheFirstValue();
        final int end = start + editors.valueAt(0).orElseThrow().length();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> values().getDocument().replaceString(start, end, value));
    }

    // UC-EDITOR-PANEL-007
    public void testTheCursorRestsOnlyInsideAValue() {
        values().getCaretModel().moveToOffset(1);
        assertEquals("the cursor rested outside every value", List.of(startOfTheFirstValue()), caretOffsets(values()));

        values().getCaretModel().moveToOffset(JSON.length());
        assertEquals("the cursor rested outside every value", List.of(endOf(1)), caretOffsets(values()));
    }

    // UC-EDITOR-PANEL-007
    public void testAllCaretsPutsACursorAtTheEndOfEveryValueAndTheLeftMarksEachCursorsLine() {
        editors.caretOnEveryValue();

        assertEquals("a cursor is not at the end of every value", List.of(endOf(0), endOf(1)), caretOffsets(values()));
        assertEquals("the values under the cursors are not both, last first", List.of(1, 0), editors.indicesUnderCarets());
        assertEquals("the left side does not mark each cursor's line", List.of(2, 5),
                Stream.of(originals().getMarkupModel().getAllHighlighters()).filter(highlighter -> highlighter.getLayer() == HighlighterLayer.CARET_ROW)
                        .map(highlighter -> originals().getDocument().getLineNumber(highlighter.getStartOffset())).sorted().toList());
    }

    // Rule-EDITOR-PANEL-298
    public void testNextAndPreviousWrapFromTheLastValueToTheFirst() {
        editors.focusValue(1);

        editors.navigate(1);
        assertEquals("Next on the last value did not wrap to the first", List.of(endOf(0)), caretOffsets(values()));

        editors.navigate(-1);
        assertEquals("Previous on the first value did not wrap to the last", List.of(endOf(1)), caretOffsets(values()));
    }

    // Rule-EDITOR-PANEL-043
    public void testAnEditedValueTurnsGreenAndLosesItWhenItIsTheOriginalAgain() {
        typedIntoTheFirstValue("Sign in");
        Await.until("an edited value did not turn green", () -> greenIn(values()).size() == 1);

        typedIntoTheFirstValue("Login");
        Await.until("a value put back as it was stayed green", () -> greenIn(values()).isEmpty());
    }

    // UC-EDITOR-PANEL-007
    public void testCtrlClickAddsACursorAndCtrlClickOnItTakesItAway() {
        editors.focusValue(0);

        pressed(values(), endOf(1), true);
        assertEquals("Ctrl+click did not add a cursor", 2, values().getCaretModel().getCaretCount());

        pressed(values(), endOf(1), true);
        assertEquals("Ctrl+click on a cursor did not take it away", List.of(endOf(0)), caretOffsets(values()));
    }

    // UC-EDITOR-PANEL-007
    public void testTheLeftSideTakesNoCursorFromAClick() {
        final @NotNull List<Integer> before = caretOffsets(originals());

        pressed(originals(), endOf(1), false);

        assertEquals("a click moved the cursor on the side that cannot be typed into", before, caretOffsets(originals()));
    }
}
