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

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.List;

public class CardWrapIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String LONG = "Invocation 4, all pass - expect stay through 4 invocations, then Passed.";

    private static @NotNull RunItem passed(final @NotNull TestCase testCase) {
        return RunItem.builder().id(testCase.getId()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build();
    }

    private static int heightAt(final @NotNull JBList<TestCase> list, final int width) {
        list.setSize(width, 4000);
        ((CollectionListModel<TestCase>) list.getModel()).allContentsChanged();
        return list.getCellBounds(1, 1).height;
    }

    private static int lastLineBottom(final @NotNull JBList<TestCase> list) {
        final @NotNull Rectangle cell = list.getCellBounds(1, 1);
        final @NotNull Component card = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(1), 1, false, false);
        card.setBounds(0, 0, cell.width, cell.height);
        UIUtil.uiTraverser(card).filter(Container.class).forEach(Container::doLayout);

        return UIUtil.uiTraverser(card).filter(JLabel.class).filter(Component::isVisible)
                .map(label -> SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), card))
                .map(at -> at.y + at.height)
                .reduce(0, Math::max);
    }

    // Rule-EDITOR-PANEL-003
    public void testANarrowerEditorGivesAWrappedTitleItsExtraLineEvenWhenTheCardAboveSaysTheSame() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull List<TestCase> testCases = List.of(
                EditorFixtures.testCase(getProject(), EditorFixtures.testSet(getProject(), testProject, "Login"), LONG, "1"),
                EditorFixtures.testCase(getProject(), EditorFixtures.testSet(getProject(), testProject, "Logout"), LONG, "1"));
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRun(getProject(), testProject, testCases.stream().map(CardWrapIdeTest::passed).toList()), getTestRootDisposable());
        final @NotNull JBList<TestCase> list = editor.getList();

        final int wide = heightAt(list, 1400);
        final int narrow = heightAt(list, 420);

        assertTrue("the card kept the height it had when its title fit on one line: " + wide + " then " + narrow, narrow > wide);
        assertTrue("the card's last line is drawn below its own bottom edge", lastLineBottom(list) <= narrow);
    }
}
