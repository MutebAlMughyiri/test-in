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
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.TestCases;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testrun.form.TestRunFormFilter;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TestRunFormFilterIdeTest extends AbstractTempRootIdeTest {

    private TestCaseDto urgent;
    private TestCaseDto later;
    private TestCaseDto paid;
    private TestRunFormFilter filter;

    private static @NotNull CheckedTreeNode holding(final @NotNull TestSetNode testSet, final @NotNull TestCaseDto... testCases) {
        final @NotNull CheckedTreeNode node = new CheckedTreeNode(testSet);
        for (final TestCaseDto tc : testCases) node.add(new CheckedTreeNode(tc));
        return node;
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
        urgent = aTestCase(login, Priority.HIGH, "a");
        later = aTestCase(login, Priority.LOW, "b");
        paid = aTestCase(card, Priority.LOW, "c");

        final @NotNull CheckedTreeNode top = new CheckedTreeNode(testProject.getTestCasesFolder());
        top.add(holding(login, urgent, later));
        top.add(holding(card, paid));
        filter = new TestRunFormFilter(getProject(), testProject.getTestCasesFolder().getPath(), top);
    }

    private @NotNull TestCaseDto aTestCase(final @NotNull TestSetNode testSet, final @NotNull Priority priority, final @NotNull String order) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Pay with a " + priority + " card").order(order).priority(priority).build();
        tc.setParent(testSet);
        assertTrue(Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), tc));
        return tc;
    }

    private @NotNull FilterPopupBtn button() {
        return Drawn.first(filter.getSelection().getPanel(), FilterPopupBtn.class);
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
            if (held instanceof TestCaseDto) leaves.add(held);
        }
        return leaves;
    }

    // Rule-TREE-PANEL-129, Rule-EDITOR-PANEL-094
    public void testTheFilterMenuIsTheEditorsOne() {
        final @NotNull List<String> names = menu().stream().map(TestRunFormFilterIdeTest::nameOf).toList();

        assertTrue("the Filter menu has no priority filter: " + names, names.contains(TestSetEditorAttributes.PRIORITY.getName()));
        assertTrue("the Filter menu has no group filter: " + names, names.contains(TestSetEditorAttributes.GROUP.getName()));
        assertTrue("the Filter menu has no module filter: " + names, names.contains(TestSetEditorAttributes.MODULE.getName()));
        assertTrue("the Filter menu has no automation filter: " + names, names.contains(Bundle.message("filter.automation")));
        assertTrue("the Filter menu has no status filter: " + names, names.contains(Bundle.message("filter.test.case.status")));
        assertTrue("the Filter menu is not the editors' one: " + names, names.contains(Bundle.message("filter.run.item.status")));
        assertEquals("Test Set does not offer the test sets the tree holds", List.of("Login", "Card"), entriesOf(Bundle.message("filter.test.set")).stream().map(TestRunFormFilterIdeTest::nameOf).toList());
    }

    // Rule-TREE-PANEL-129
    public void testAPriorityFilterPicksTheSameTestCasesAsInAnEditor() {
        final @NotNull ToggleAction high = entriesOf(TestSetEditorAttributes.PRIORITY.getName()).stream()
                .filter(entry -> nameOf(entry).equals(Priority.HIGH.getLabel()))
                .map(ToggleAction.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the priority filter does not offer " + Priority.HIGH.getLabel()));
        assertEquals(List.of(urgent, later, paid).size(), shown().size());

        high.setSelected(TestActionEvent.createTestEvent(high), true);

        assertEquals("the priority filter did not pick the test cases an editor's would", TestCaseFilter.filter(List.of(urgent, later, paid), FilterAndSortChoice.of(button(), "")), shown());
        assertEquals(List.of(urgent), shown());
    }
}
