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

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.TestCase;
import org.testin.model.status.TestCaseStatus;
import org.testin.util.FixedColors;
import org.testin.view.Drawn;

import javax.swing.JTextArea;
import java.awt.Color;
import java.util.Set;

public class DescriptionColorIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestCaseCard bound(final @NotNull TestCaseStatus status) {
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        final @NotNull TestCase testCase = TestCase.builder().description("Log in with a locked user").status(status).build();
        card.updateData(0, testCase, Set.of(), testCase.getDescription());
        return card;
    }

    private @NotNull Color descriptionOf(final @NotNull TestCaseStatus status) {
        return Drawn.first(bound(status), JTextArea.class).getForeground();
    }

    private boolean saysToBeUpdated(final @NotNull TestCaseStatus status) {
        return Drawn.words(bound(status)).contains(TestCaseStatus.TO_BE_UPDATED.getLabel());
    }

    // Rule-EDITOR-PANEL-294
    public void testAReviewedTestCaseIsWhiteWithALittleYellow() {
        assertEquals(FixedColors.CREAM, descriptionOf(TestCaseStatus.REVIEWED));
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
    public void testTheColorFollowsTheCardWhenItIsReusedForAnotherTestCase() {
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        card.updateData(0, TestCase.builder().description("Pay with a saved card").status(TestCaseStatus.DISABLED).build(), Set.of(), "Pay with a saved card");

        card.updateData(1, TestCase.builder().description("Pay with a new card").status(TestCaseStatus.PENDING).build(), Set.of(), "Pay with a new card");

        assertEquals("a reused card kept the previous test case's gray", UIUtil.getLabelForeground(), Drawn.first(card, JTextArea.class).getForeground());
    }
}