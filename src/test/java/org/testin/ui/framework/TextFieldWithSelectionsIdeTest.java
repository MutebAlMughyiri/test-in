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

import com.intellij.icons.AllIcons;
import com.intellij.ui.TextIcon;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Await;
import org.testin.OnScreen;
import org.testin.view.Drawn;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TextFieldWithSelectionsIdeTest extends AbstractIdeTest {

    private static final @NotNull List<String> TEST_SETS = List.of("Login", "Payments", "Pay later");

    private final @NotNull List<String> asked = new CopyOnWriteArrayList<>();

    private @NotNull Answer<String> theTestSetsMatching(final @NotNull String query) {
        asked.add(query);
        final @NotNull List<Selection<String>> found = TEST_SETS.stream().filter(name -> name.regionMatches(true, 0, query, 0, query.length())).map(name -> Selection.of(AllIcons.Nodes.Folder, name, "", name)).toList();
        return new Answer<>(found, query.isEmpty() ? "" : found.size() + " found");
    }

    private static @NotNull List<String> rowsShown(final @NotNull JBList<?> list) {
        return IntStream.range(0, list.getModel().getSize()).mapToObj(row -> ((Selection<?>) list.getModel().getElementAt(row)).name()).toList();
    }

    private static @NotNull List<String> notesOn(final @NotNull ExtendableTextField field) {
        return field.getExtensions().stream().map(extension -> extension.getIcon(false)).filter(TextIcon.class::isInstance).map(icon -> ((TextIcon) icon).getText()).toList();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-073
    public void testTheRowsAreAskedForOnceShownAndAgainForWhatIsTypedWithHowManyMatched() {
        final @NotNull TextFieldWithSelections<String> search = new TextFieldWithSelections<>(AllIcons.Nodes.Folder, "Find a test set", List.of(), this::theTestSetsMatching, 2);
        final @NotNull JBList<?> list = Drawn.first(search.getPanel(), JBList.class);
        final @NotNull ExtendableTextField field = Drawn.first(search.getPanel(), ExtendableTextField.class);

        OnScreen.shown(search.getPanel(), getTestRootDisposable());

        Await.until("the rows were not asked for once the field was shown", () -> rowsShown(list).equals(TEST_SETS));
        assertEquals("the first row is not chosen to start with", Optional.of("Login"), search.selection());

        field.setText("Pay");

        Await.until("the rows were not asked for again for what was typed", () -> rowsShown(list).equals(List.of("Payments", "Pay later")));
        assertEquals("the field was not asked once on showing and once for what was typed", List.of("", "Pay"), asked);
        assertEquals("the field does not say how many matched beside what was typed", List.of("2 found"), notesOn(field));
    }

    // UC-INTERNAL-001
    public void testWhatMatchesNothingShowsNoRowAndTheArrowsAndAClickDoNothing() {
        final @NotNull TextFieldWithSelections<String> search = new TextFieldWithSelections<>(AllIcons.Nodes.Folder, "Find a test set", List.of(), this::theTestSetsMatching, 2);
        final @NotNull JBList<?> list = Drawn.first(search.getPanel(), JBList.class);
        final @NotNull ExtendableTextField field = Drawn.first(search.getPanel(), ExtendableTextField.class);
        final @NotNull AtomicInteger submitted = new AtomicInteger();
        search.onSubmitRequest(submitted::incrementAndGet);
        OnScreen.shown(search.getPanel(), getTestRootDisposable());
        Await.until("the rows were not asked for once the field was shown", () -> rowsShown(list).equals(TEST_SETS));

        field.setText("Reports");
        Await.until("a query matching nothing still shows rows", () -> rowsShown(list).isEmpty());
        field.getActionMap().get("testin.framework.selectionDown").actionPerformed(new ActionEvent(field, 0, ""));
        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1));

        assertEquals("a query matching nothing left a row chosen", Optional.empty(), search.selection());
        assertEquals("a click on a list with no row submitted", 0, submitted.get());
    }

    // UC-INTERNAL-001
    public void testTheArrowsMoveOverRowsThatSayNothingBesideTheirName() {
        final @NotNull TextFieldWithSelections<String> search = new TextFieldWithSelections<>(AllIcons.Nodes.Folder, "Find a test set", List.of(), this::theTestSetsMatching, 2);
        final @NotNull JBList<?> list = Drawn.first(search.getPanel(), JBList.class);
        final @NotNull ExtendableTextField field = Drawn.first(search.getPanel(), ExtendableTextField.class);
        OnScreen.shown(search.getPanel(), getTestRootDisposable());
        Await.until("the rows were not asked for once the field was shown", () -> rowsShown(list).equals(TEST_SETS));

        field.getActionMap().get("testin.framework.selectionDown").actionPerformed(new ActionEvent(field, 0, ""));

        assertEquals("the down arrow did not move to the second row", Optional.of("Payments"), search.selection());
    }

    // UC-INTERNAL-007
    public void testAnEmptyFieldSaysWhatIsMissingInItsOwnPlace() {
        final @NotNull TextFieldWithSelections<String> search = new TextFieldWithSelections<>(AllIcons.Nodes.Folder, "Find a test set", List.of(), this::theTestSetsMatching, 2);
        final @NotNull ExtendableTextField field = Drawn.first(search.getPanel(), ExtendableTextField.class);

        search.showEmptyWarning();

        assertEquals("an empty field does not say what is missing", "Find a test set", field.getEmptyText().getText());
    }
}
