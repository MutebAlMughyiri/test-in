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

package org.testin.testcase.update;

import com.intellij.ui.components.fields.IntegerField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.filter.SortDirection;
import org.testin.filter.SortField;
import org.testin.filter.SortPopupBtn;
import org.testin.filter.Sorting;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class OrderByNumberIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError(file + " could not be read", ex);
        }
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);
        super.tearDown();
    }

    private @NotNull TestSetEditor sixTestCases() {
        testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 6);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private void select(final @NotNull TestSetEditor editor, final @NotNull String description) {
        for (int i = 0; i < editor.getList().getModel().getSize(); i++)
            if (editor.getList().getModel().getElementAt(i).getDescription().equals(description))
                editor.getList().setSelectedIndex(i);
    }

    private @NotNull IntegerField theOrderBox(final @NotNull TestSetEditor editor) {
        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> UpdateTestCaseAction.openField(getProject(), editor, UpdateTestCaseFields.ORDER));
        return Drawn.components(ShownDialog.content(getProject(), UpdateTestCaseDialog.class)).stream()
                .filter(IntegerField.class::isInstance).map(IntegerField.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("the order dialog has no box for the number"));
    }

    private @NotNull List<String> storedOrder() {
        return TestCaseOrder.ordered(Services.getInstance(getProject(), TestCases.class).getTestCasesForTestSet(testSet.getPath())).stream().map(TestCaseDto::getDescription).map(description -> description.replace("Test case number ", "")).toList();
    }

    private void moveToThird(final @NotNull TestSetEditor editor, final @NotNull String description) {
        select(editor, description);
        theOrderBox(editor).setValue(3);
        ShownDialog.press(getProject(), UpdateTestCaseDialog.class, Shortcuts.Enter.getKey());
    }

    private @NotNull List<String> everyFileButNumber6() {
        try (final Stream<Path> files = Files.list(testSet.getPath())) {
            return files.filter(file -> file.toString().endsWith(".tc")).sorted().map(file -> file + "@" + file.toFile().lastModified() + "=" + read(file)).filter(file -> !file.contains("\"Test case number 6\"")).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    // Rule-EDITOR-PANEL-054
    public void testTheNumberIsThePositionInTheWholeTestSet() {
        final @NotNull TestSetEditor editor = sixTestCases();
        editor.getToolBar().getSearchTxt().setText("number 5");
        Await.until("the search never narrowed the list to one", () -> editor.getList().getModel().getSize() == 1);

        select(editor, "Test case number 5");

        assertEquals("the box does not hold the position in the whole test set, counting from one", Integer.valueOf(5), theOrderBox(editor).getValue());
    }

    // Rule-EDITOR-PANEL-055, Rule-EDITOR-PANEL-056
    public void testTypingThreePutsItThirdAndOnlyItsOwnFileIsWritten() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull List<String> others = everyFileButNumber6();

        moveToThird(editor, "Test case number 6");

        Await.until("the test case never moved up to third: " + storedOrder(), () -> storedOrder().equals(List.of("1", "2", "6", "3", "4", "5")));
        assertEquals("a test case around the moved one was written", others, everyFileButNumber6());
    }

    // Rule-EDITOR-PANEL-055
    public void testMovingDownCountsWithTheTestCaseTakenOut() {
        final @NotNull TestSetEditor editor = sixTestCases();

        moveToThird(editor, "Test case number 1");

        Await.until("typing three did not put the test case third: " + storedOrder(), () -> storedOrder().equals(List.of("2", "3", "1", "4", "5", "6")));
    }

    // Rule-EDITOR-PANEL-282
    public void testUnderASortTheBoxHoldsThePlaceInTheSetAndThatIsWhatIsWritten() {
        final @NotNull TestSetEditor editor = sixTestCases();
        final @NotNull SortPopupBtn sort = editor.getToolBar().getToolbarItem(SortPopupBtn.class);
        Sorting.choose(sort, SortField.DESCRIPTION.getLabel());
        Sorting.choose(sort, SortDirection.DESCENDING.getLabel());
        Await.until("the sort never put number 6 first", () -> editor.getList().getModel().getSize() > 0 && editor.getList().getModel().getElementAt(0).getDescription().equals("Test case number 6"));

        select(editor, "Test case number 6");
        assertEquals("under a sort the box does not hold the place in the set", Integer.valueOf(6), theOrderBox(editor).getValue());
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);

        moveToThird(editor, "Test case number 6");

        Await.until("the place in the set was not what was written: " + storedOrder(), () -> storedOrder().equals(List.of("1", "2", "6", "3", "4", "5")));
    }
}
