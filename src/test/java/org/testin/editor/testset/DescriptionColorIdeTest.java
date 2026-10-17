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

package org.testin.editor.testset;

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.testrun.RunItemCard;
import org.testin.model.TestCase;
import org.testin.model.status.TestCaseStatus;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.form.TestCaseSelectionRenderer;
import org.testin.util.FixedColors;
import org.testin.view.Drawn;
import org.testin.view.details.Title;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DescriptionColorIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestCaseCard bound(final @NotNull TestCaseStatus testCaseStatus) {
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        final @NotNull TestCase testCase = TestCase.builder().description("Log in with a locked user").status(testCaseStatus).build();
        card.updateData(0, testCase, Set.of(), testCase.getDescription());
        return card;
    }

    private @NotNull Color descriptionOf(final @NotNull TestCaseStatus testCaseStatus) {
        return Drawn.first(bound(testCaseStatus), JTextArea.class).getForeground();
    }

    private boolean saysToBeUpdated(final @NotNull TestCaseStatus testCaseStatus) {
        return Drawn.words(bound(testCaseStatus)).contains(TestCaseStatus.TO_BE_UPDATED.getLabel());
    }

    // Rule-EDITOR-PANEL-294
    public void testAReviewedTestCaseIsWhiteWithALittleBlue() {
        assertEquals(FixedColors.PALE_BLUE, descriptionOf(TestCaseStatus.REVIEWED));
    }

    // Rule-EDITOR-PANEL-294
    public void testPendingAndToBeUpdatedKeepTheEditorsTextColor() {
        assertEquals(UIUtil.getLabelForeground(), descriptionOf(TestCaseStatus.PENDING));
        assertEquals(UIUtil.getLabelForeground(), descriptionOf(TestCaseStatus.TO_BE_UPDATED));
    }

    // Rule-EDITOR-PANEL-294
    public void testADisabledTestCaseIsGrayAsInTheTestRunForm() {
        assertEquals(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor(), descriptionOf(TestCaseStatus.DISABLED));
    }

    // Rule-EDITOR-PANEL-294
    public void testOnlyAToBeUpdatedTestCaseCarriesTheBadgeWhateverTheFields() {
        assertTrue("a To Be Updated test case has no badge", saysToBeUpdated(TestCaseStatus.TO_BE_UPDATED));
        assertFalse("a Reviewed test case carries the To Be Updated badge", saysToBeUpdated(TestCaseStatus.REVIEWED));
    }

    // Rule-EDITOR-PANEL-294
    public void testARunItemsCardDrawsItsTestCasesDescriptionInTheStatusColor() {
        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a locked user").status(TestCaseStatus.REVIEWED).build();
        final @NotNull RunItem runItem = new RunItem().setUuid(testCase.getUuid()).showing(Optional.of(testCase), Optional.empty(), Optional.empty(), false);
        final @NotNull RunItemCard card = new RunItemCard(getProject());

        card.updateData(0, Set.of(), runItem, testCase.getDescription());

        assertEquals("a test run's card does not draw a Reviewed description in its color", FixedColors.PALE_BLUE, Drawn.first(card, JTextArea.class).getForeground());
    }

    // Rule-EDITOR-PANEL-294
    public void testTheViewPanelAndTheCreateTestRunTreeDrawADescriptionInItsStatusColor() {
        final @NotNull TestCase reviewed = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a locked user").status(TestCaseStatus.REVIEWED).build();
        final @NotNull JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        new Title().render(getProject(), panel, new GridBagConstraints(), reviewed, 0);

        assertEquals("the view panel does not draw a Reviewed description in its color", FixedColors.PALE_BLUE, Drawn.first(panel, JTextArea.class).getForeground());

        final @NotNull CheckboxTree.CheckboxTreeCellRenderer renderer = TestCaseSelectionRenderer.create();
        renderer.getTreeCellRendererComponent(new CheckboxTree(renderer, new CheckedTreeNode("Test Cases")), new CheckedTreeNode(reviewed), false, false, true, 0, false);
        final @NotNull SimpleColoredComponent.ColoredIterator fragments = renderer.getTextRenderer().iterator();
        fragments.next();

        assertEquals("the Create Test Run tree does not draw a Reviewed description in its color", FixedColors.PALE_BLUE, fragments.getTextAttributes().getFgColor());
    }

    // Rule-EDITOR-PANEL-294
    public void testTheColorFollowsTheCardWhenItIsReusedForAnotherTestCase() {
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        card.updateData(0, TestCase.builder().description("Pay with a saved card").status(TestCaseStatus.DISABLED).build(), Set.of(), "Pay with a saved card");

        card.updateData(1, TestCase.builder().description("Pay with a new card").status(TestCaseStatus.PENDING).build(), Set.of(), "Pay with a new card");

        assertEquals("a reused card kept the previous test case's gray", UIUtil.getLabelForeground(), Drawn.first(card, JTextArea.class).getForeground());
    }
}