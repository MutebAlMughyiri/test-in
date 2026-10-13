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

package org.testin.testcase;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.ShownFields;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.FileKind;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.view.Drawn;

import javax.swing.ListModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class TestCaseNumberIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull Set<TestSetEditorAttributes> shownBefore = Set.of();

    private static int numberOnTheCard(final @NotNull TestSetEditor editor, final @NotNull TestCaseDto tc) {
        final @NotNull String title = editor.cardTitle(tc);
        return Integer.parseInt(title.substring(0, title.indexOf('.')));
    }

    private static @NotNull List<TestCaseDto> cardsDrawn(final @NotNull TestSetEditor editor) {
        final @NotNull ListModel<?> cards = Drawn.components(editor.getComponent()).stream().filter(JBList.class::isInstance).map(JBList.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("the editor draws no card list")).getModel();
        final @NotNull List<TestCaseDto> drawn = new ArrayList<>();
        IntStream.range(0, cards.getSize()).forEach(i -> drawn.add((TestCaseDto) cards.getElementAt(i)));
        return drawn;
    }

    private static int numberInTheGrid(final @NotNull TestSetEditor editor, final @NotNull TestCaseDto tc) {
        editor.onToolBarSwitchedToGridView();
        final @NotNull JBTable grid = Drawn.components(editor.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("the editor draws no grid"));
        for (int row = 0; row < grid.getModel().getRowCount(); row++) {
            if (tc.getDescription().equals(grid.getModel().getValueAt(row, TestSetEditorAttributes.DESCRIPTION.column())))
                return Integer.parseInt(String.valueOf(grid.getModel().getValueAt(row, TestSetEditorAttributes.ORDER.column())));
        }
        throw new AssertionError("the grid has no row for '" + tc.getDescription() + "'");
    }

    private static void filteredTo(final @NotNull TestSetEditor editor, final @NotNull String query) {
        editor.getToolBar().getSearchTxt().setText(query);
        editor.onToolBarSearchValueChanged();
    }

    private static byte @NotNull [] fileOf(final @NotNull TestCaseDto tc) {
        final @NotNull Path file = tc.getParent().getPath().resolve(FileKind.TEST_CASE.fileName(tc.getId()));
        try {
            return Files.readAllBytes(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        shownBefore = ShownFields.read(ShownFields.IN_TEST_SETS, TestSetEditorAttributes.class);
        ShownFields.write(ShownFields.IN_TEST_SETS, EnumSet.allOf(TestSetEditorAttributes.class));
    }

    @Override
    protected void tearDown() {
        ShownFields.write(ShownFields.IN_TEST_SETS, shownBefore);
        super.tearDown();
    }

    private @NotNull TestSetEditor opened(final @NotNull TestSetNode testSet) {
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    // UC-INTERNAL-004, Rule-INTERNAL-025
    public void testATestCasesNumberIsItsPlaceInItsTestSetCountingFromOne() {
        final @NotNull TestSetNode payment = indexedTestSet("Payment", theTestCasesDirectory());
        final @NotNull TestCaseDto accepted = indexedTestCase(payment, "Card is accepted", "d");
        final @NotNull TestCaseDto declined = indexedTestCase(payment, "Card is declined", "b");
        final @NotNull TestCaseDto expired = indexedTestCase(payment, "Card has expired", "c");

        final @NotNull TestSetEditor editor = opened(payment);

        assertEquals("the first test case in the test set is not number one", 1, numberOnTheCard(editor, declined));
        assertEquals(2, numberOnTheCard(editor, expired));
        assertEquals("the number follows the order the test cases were added in, not their place in the test set", 3, numberOnTheCard(editor, accepted));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-026
    public void testTheNumberIsWorkedOutWhenDrawnAndStoredNowhere() {
        final @NotNull TestSetNode payment = indexedTestSet("Payment", theTestCasesDirectory());
        final @NotNull TestCaseDto declined = indexedTestCase(payment, "Card is declined", "b");
        indexedTestCase(payment, "Card is accepted", "c");
        final @NotNull TestSetEditor editor = opened(payment);
        assertEquals(1, numberOnTheCard(editor, declined));
        final byte @NotNull [] before = fileOf(declined);

        indexedTestCase(payment, "Card is stolen", "a");
        editor.reloadData();

        Await.until("the test case was not renumbered when a test case was put above it", () -> numberOnTheCard(editor, declined) == 2);
        assertEquals("a test case's file was written because another test case moved its number", new String(before, StandardCharsets.UTF_8), new String(fileOf(declined), StandardCharsets.UTF_8));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-027
    public void testTheCardTheGridRowAndTheGeneratedMethodReadTheSameNumber() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto open = createdTestCase(login, "Open the start page", "b");
        final @NotNull TestCaseDto logIn = createdTestCase(login, "Log in with a valid user", "c");
        final @NotNull TestCaseDto logOut = createdTestCase(login, "Log out", "d");

        final @NotNull TestSetEditor editor = opened(login);
        filteredTo(editor, "Log");

        for (final TestCaseDto tc : List.of(logIn, logOut)) {
            final int onTheCard = numberOnTheCard(editor, tc);
            assertEquals("the grid row and the card disagree on '" + tc.getDescription() + "'", onTheCard, numberInTheGrid(editor, tc));
            assertEquals("the generated method and the card disagree on '" + tc.getDescription() + "'", String.valueOf(onTheCard), attributeOf(writtenMethodOf(LOGIN_TEST, tc), "priority"));
        }
        assertEquals("1", attributeOf(writtenMethodOf(LOGIN_TEST, open), "priority"));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-028
    public void testAFilterNeverRenumbers() {
        final @NotNull TestSetNode payment = indexedTestSet("Payment", theTestCasesDirectory());
        indexedTestCase(payment, "Card is declined", "b");
        indexedTestCase(payment, "Card has expired", "c");
        final @NotNull TestCaseDto accepted = indexedTestCase(payment, "Card is accepted", "d");
        final @NotNull TestSetEditor editor = opened(payment);

        filteredTo(editor, "accepted");

        assertEquals("the filter did not hide the other test cases", List.of(accepted), cardsDrawn(editor));
        assertEquals("hiding rows renumbered the card that remains", 3, numberOnTheCard(editor, accepted));
        assertEquals("hiding rows renumbered the grid row that remains", 3, numberInTheGrid(editor, accepted));
    }
}
