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

package org.testin.filter;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionHolder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.UiInterceptors;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.TestCases;
import org.testin.model.Automated;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.form.TestRunFormFilter;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TestRunFormFilterIdeTest extends AbstractTempRootIdeTest {

    private TestCase urgent;

    private TestCase later;

    private TestCase paid;

    private TestRunFormFilter filter;

    private static @NotNull CheckedTreeNode holding(final @NotNull TestSetNode testSet, final @NotNull TestCase... testCases) {
        final @NotNull CheckedTreeNode treeNode = new CheckedTreeNode(testSet);
        for (final TestCase testCase : testCases) treeNode.add(new CheckedTreeNode(testCase));
        return treeNode;
    }

    private static @NotNull String nameOf(final @NotNull AnAction entry) {
        return Objects.requireNonNullElse(entry.getTemplatePresentation().getText(), "");
    }

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = made.testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestSetNode card = made.testSet(testProject.getTestCasesFolder(), "Card");
        urgent = aTestCase(login, Priority.HIGH, "a", "SMOKE");
        later = aTestCase(login, Priority.LOW, "b", "SMOKE", "REGRESSION");
        paid = aTestCase(card, Priority.LOW, "c");

        final @NotNull CheckedTreeNode top = new CheckedTreeNode(testProject.getTestCasesFolder());
        top.add(holding(login, urgent, later));
        top.add(holding(card, paid));
        filter = new TestRunFormFilter(getProject(), testProject.getTestCasesFolder().getPath(), top);
    }

    private @NotNull TestCase aTestCase(final @NotNull TestSetNode testSet, final @NotNull Priority priority, final @NotNull String order, final @NotNull String... groups) {
        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Pay with a " + priority + " card").order(order).priority(priority).groups(new ArrayList<>(List.of(groups))).build();
        testCase.setParent(testSet);
        assertTrue(Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase));
        return testCase;
    }

    private @NotNull FilterPopupButton button() {
        return Drawn.first(filter.getSelection().getPanel(), FilterPopupButton.class);
    }

    private @NotNull List<AnAction> menu() {
        return MenuChildren.of(button().menu());
    }

    private @NotNull List<AnAction> entriesOf(final @NotNull String submenu) {
        final @NotNull AnAction found = menu().stream().filter(entry -> nameOf(entry).equals(submenu)).findFirst()
                .orElseThrow(() -> new AssertionError("the Filter menu has no " + submenu + ": " + menu().stream().map(TestRunFormFilterIdeTest::nameOf).toList()));
        return MenuChildren.of((ActionGroup) found);
    }

    private @NotNull List<Object> shown() {
        final @NotNull CheckboxTree tree = Drawn.first(filter.getSelection().getPanel(), CheckboxTree.class);
        final @NotNull List<Object> leaves = new ArrayList<>();
        for (final Object each : Collections.list(((CheckedTreeNode) tree.getModel().getRoot()).depthFirstEnumeration())) {
            final @NotNull Object held = ((CheckedTreeNode) each).getUserObject();
            if (held instanceof TestCase) leaves.add(held);
        }
        return leaves;
    }

    // Rule-EDITOR-PANEL-095, Rule-TREE-PANEL-129
    public void testTheGroupFilterOffersTheGroupsOfTheTreesTestCasesWithNoEditorOpen() {
        assertEquals("the Group filter does not offer the groups the tree's test cases carry", List.of(Groups.NONE, "REGRESSION", "SMOKE"), entriesOf(TestSetEditorFields.GROUP.getName()).stream().map(TestRunFormFilterIdeTest::nameOf).toList());
    }

    // Rule-TREE-PANEL-129, Rule-EDITOR-PANEL-094
    public void testTheFilterMenuIsTheEditorsOne() {
        final @NotNull List<String> names = menu().stream().map(TestRunFormFilterIdeTest::nameOf).toList();

        assertTrue("the Filter menu has no priority filter: " + names, names.contains(TestSetEditorFields.PRIORITY.getName()));
        assertTrue("the Filter menu has no group filter: " + names, names.contains(TestSetEditorFields.GROUP.getName()));
        assertTrue("the Filter menu has no module filter: " + names, names.contains(TestSetEditorFields.MODULE.getName()));
        assertTrue("the Filter menu has no automation filter: " + names, names.contains(Bundle.message("filter.automation")));
        assertTrue("the Filter menu has no status filter: " + names, names.contains(Bundle.message("filter.test.case.status")));
        assertTrue("the Filter menu is not the editors' one: " + names, names.contains(Bundle.message("filter.run.item.status")));
        assertEquals("Test Set does not offer the test sets the tree holds", List.of("Login", "Card"), entriesOf(Bundle.message("filter.test.set")).stream().map(TestRunFormFilterIdeTest::nameOf).toList());
    }

    // Rule-TREE-PANEL-129
    public void testAPriorityFilterPicksTheSameTestCasesAsInAnEditor() {
        assertEquals(List.of(urgent, later, paid).size(), shown().size());

        Filtering.choose(button(), TestSetEditorFields.PRIORITY.getName(), Priority.HIGH.getLabel());

        assertEquals("the priority filter did not pick the test cases an editor's would", TestCaseFilter.filter(List.of(urgent, later, paid), FilterAndSortChoice.of(button(), ""), _ -> Optional.empty(), _ -> Automated.UNKNOWN), shown());
        assertEquals(List.of(urgent), shown());
    }

    // UC-EDITOR-PANEL-020, Rule-TREE-PANEL-129
    public void testClickingFilterOpensItsMenuUnderTheButton() {
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });

        button().doClick();

        try {
            assertEquals("clicking Filter did not open one menu", 1, shown.size());
            final @NotNull List<?> items = ((ListPopup) shown.getFirst()).getListStep().getValues();
            final @NotNull List<String> entries = items.stream().map(AnActionHolder.class::cast).map(AnActionHolder::getAction).map(TestRunFormFilterIdeTest::nameOf).toList();
            assertTrue("the menu Filter opened is not the filter menu: " + entries, entries.contains(TestSetEditorFields.PRIORITY.getName()));
        } finally {
            shown.forEach(Disposer::dispose);
        }
    }

    // UC-EDITOR-PANEL-020, Rule-TREE-PANEL-129
    public void testUntickingTheOnlyPickShowsEveryTestCaseAgain() {
        Filtering.choose(button(), TestSetEditorFields.PRIORITY.getName(), Priority.HIGH.getLabel());
        assertEquals(List.of(urgent), shown());

        Filtering.untick(button(), TestSetEditorFields.PRIORITY.getName(), Priority.HIGH.getLabel());

        assertEquals("unticking the only pick did not show every test case again", List.of(urgent, later, paid), shown());
        assertEquals("the unticked pick is still counted as a filter", 0, button().activeFilterCount());
    }

    // Rule-TREE-PANEL-129, Rule-EDITOR-PANEL-099
    public void testResettingTheFiltersShowsEveryTestCaseAgain() {
        Filtering.choose(button(), TestSetEditorFields.PRIORITY.getName(), Priority.HIGH.getLabel());
        assertEquals(List.of(urgent), shown());

        button().resetToolBarFilter();

        assertEquals("resetting the filters did not show every test case again", List.of(urgent, later, paid), shown());
    }
}
