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

package org.testin.search;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.NodesOnDisk;
import org.testin.explorer.tree.TreePanelCellRenderer;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.model.node.Node;
import org.testin.model.node.TestProjectNode;
import org.testin.ui.framework.Answer;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.SelectionList;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTree;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class SearchRowsIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull TestProjectNode nafath = new TestProjectNode();

    private static @NotNull JBList<?> drawnAsTheSearchDrawsThem(final @NotNull List<SelectionList<Hit>> rows) {
        final @NotNull ComponentDialogBase.TextFieldBuilder<Hit> builder = ComponentDialogBase.<Hit>textFieldWithSelections().placeholder("search..");
        rows.forEach(row -> builder.selection(row.icon(), row.name(), row.hint(), row.value()));
        final @NotNull TextFieldWithSelections<Hit> search = builder.build().getComponent();
        return Drawn.components(search.getPanel()).stream().filter(JBList.class::isInstance).map(JBList.class::cast).findFirst().orElseThrow();
    }

    private static <E> @NotNull SimpleColoredComponent rowAt(final @NotNull JList<E> list, final int index) {
        final @NotNull Component drawn = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(index), index, false, false);
        final @NotNull SimpleColoredComponent row = (SimpleColoredComponent) drawn;
        row.setSize(900, row.getPreferredSize().height);
        return row;
    }

    private static @NotNull List<String> wordsOf(final @NotNull SimpleColoredComponent row) {
        final @NotNull List<String> words = new ArrayList<>();
        final SimpleColoredComponent.@NotNull ColoredIterator fragments = row.iterator();
        while (fragments.hasNext()) words.add(fragments.next().trim());
        return words.stream().filter(word -> !word.isEmpty()).toList();
    }

    private static int whereThePathStarts(final @NotNull SimpleColoredComponent row) {
        return IntStream.range(0, row.getWidth()).filter(x -> row.findFragmentAt(x) == 1).findFirst().orElseThrow(() -> new AssertionError("the row shows no path"));
    }

    @Override
    protected void setUp() {
        super.setUp();
        nafath = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
    }

    private @NotNull Icon drawnInTheTree(final @NotNull Node node) {
        final @NotNull TreePanelCellRenderer tree = new TreePanelCellRenderer(Set.of());
        tree.customizeCellRenderer(new JTree(), node, false, false, true, 0, false);
        return tree.getIcon();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-072
    public void testWhatKindOfThingARowIsItsIconSaysAndItIsTheIconTheTreeDraws() {
        final @NotNull Node login = new NodesOnDisk(getProject()).testSet(nafath.getTestCasesFolder(), "Login");
        final @NotNull Node cycle = new NodesOnDisk(getProject()).testRun(nafath.getTestRunsFolder(), "Login cycle");

        assertSame("a test set looks different in the search than in the tree", drawnInTheTree(login), Hit.of(login).icon());
        assertSame("a test run looks different in the search than in the tree", drawnInTheTree(cycle), Hit.of(cycle).icon());

        final @NotNull List<SelectionList<Hit>> rows = GlobalSearchDialog.rowsFor(getProject(), "Login").rows();
        final @NotNull JBList<?> drawn = drawnAsTheSearchDrawsThem(rows);
        for (int index = 0; index < rows.size(); index++) {
            final @NotNull Hit hit = rows.get(index).value();
            assertEquals("a word beside the name says what kind of thing the row is", List.of(hit.name(), hit.where()), wordsOf(rowAt(drawn, index)));
        }
    }

    // UC-INTERNAL-001, Rule-INTERNAL-073
    public void testTheSearchSaysHowManyMatchedNotHowManyItShows() {
        for (int set = 1; set <= 60; set++)
            new NodesOnDisk(getProject()).testSet(nafath.getTestCasesFolder(), "Login " + set);

        final @NotNull Answer<Hit> answer = GlobalSearchDialog.rowsFor(getProject(), "Login");

        assertEquals("the list does not stop at fifty", 50, answer.rows().size());
        assertEquals("the search does not say how many matched", Bundle.message("dialog.search.found", 60), answer.note());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-074
    public void testThePathSitsAgainstTheNameItBelongsToWhateverLengthTheNameIs() {
        new NodesOnDisk(getProject()).testSet(nafath.getTestCasesFolder(), "Login");
        new NodesOnDisk(getProject()).testSet(nafath.getTestCasesFolder(), "Login page");

        final @NotNull List<SelectionList<Hit>> rows = GlobalSearchDialog.rowsFor(getProject(), "Login").rows();
        final @NotNull JBList<?> drawn = drawnAsTheSearchDrawsThem(rows);
        final int shortName = IntStream.range(0, rows.size()).filter(index -> rows.get(index).name().equals("Login")).findFirst().orElseThrow();
        final int longName = IntStream.range(0, rows.size()).filter(index -> rows.get(index).name().equals("Login page")).findFirst().orElseThrow();

        assertTrue("the path is lined up into a column instead of sitting against its name", whereThePathStarts(rowAt(drawn, shortName)) < whereThePathStarts(rowAt(drawn, longName)));
    }
}
