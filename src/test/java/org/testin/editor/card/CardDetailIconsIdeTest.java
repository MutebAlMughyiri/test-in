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
package org.testin.editor.card;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testset.TestCaseCard;
import org.testin.editor.testrun.RunItemCard;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Display;
import org.testin.view.Drawn;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CardDetailIconsIdeTest extends BasePlatformTestCase {
    private static final @NotNull String EXPECTED = "The dashboard opens.";
    private static final @NotNull String REFERENCE = "JIRA-412";
    private static final @NotNull Dimension CELL = new Dimension(900, 120);

    private static @NotNull JBLabel theLineSaying(final @NotNull AbstractCard card, final @NotNull String words) {
        return Drawn.first(card, JBLabel.class, label -> label.isVisible() && Objects.requireNonNullElse(label.getText(), "").contains(words));
    }

    private @NotNull TestCaseCard aCardWithItsExpectedResult() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").expectedResult(EXPECTED).build();
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        card.updateData(0, tc, Set.of(TestSetEditorAttributes.EXPECTED_RESULT), AbstractCard.titleText(1, true, tc.getDescription()));
        card.layOutAs(CELL);
        return card;
    }

    // Rule-EDITOR-PANEL-269
    public void testADetailLineLeadsWithItsFieldsIconAndTheScreenReaderStillHearsTheName() {
        final @NotNull TestCaseCard card = aCardWithItsExpectedResult();
        final @NotNull JBLabel line = theLineSaying(card, EXPECTED);

        assertSame("the line does not lead with the field's icon", CreateTestCaseFields.EXPECTED_RESULT.getIcon(), line.getIcon());
        assertEquals("the line still names its field", EXPECTED, line.getText());
        assertTrue("a screen reader no longer hears the field's name: " + card.getAccessibleContext().getAccessibleDescription(),
                Objects.requireNonNullElse(card.getAccessibleContext().getAccessibleDescription(), "").contains(TestSetEditorAttributes.EXPECTED_RESULT.getName() + ": " + EXPECTED));
    }

    // Rule-EDITOR-PANEL-269
    public void testAReferenceLineLeadsWithItsR() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").reference(REFERENCE).build();
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        card.updateData(0, tc, Set.of(TestSetEditorAttributes.REFERENCE), AbstractCard.titleText(1, true, tc.getDescription()));
        card.layOutAs(CELL);

        final @NotNull JBLabel line = theLineSaying(card, REFERENCE);
        assertSame("the reference line does not lead with its R", UpdateTestCaseFields.REFERENCE.getIcon(), line.getIcon());
        assertEquals("the reference line shows the value alone, with no caption", REFERENCE, line.getText());
    }

    // Rule-EDITOR-PANEL-269
    public void testHoveringTheIconNamesTheField() {
        final @NotNull TestCaseCard card = aCardWithItsExpectedResult();
        final @NotNull Rectangle line = SwingUtilities.convertRectangle(theLineSaying(card, EXPECTED).getParent(), theLineSaying(card, EXPECTED).getBounds(), card);

        assertEquals(TestSetEditorAttributes.EXPECTED_RESULT.getName(), card.tooltipAt(new Point(line.x + 2, line.y + line.height / 2), CELL));
    }

    // Rule-EDITOR-PANEL-269
    public void testAFieldWithNoIconKeepsItsName() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        final @NotNull RunItem runItem = new RunItem().setId(tc.getId()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).showing(Optional.of(tc), Optional.empty(), Optional.empty(), false);
        final @NotNull RunItemCard card = new RunItemCard(getProject());
        card.updateData(0, Set.of(TestRunEditorAttributes.RUN_STATUS), runItem, AbstractCard.titleText(1, true, tc.getDescription()));

        final @NotNull JBLabel line = theLineSaying(card, RunItemStatus.FAILED.getLabel());
        assertTrue("a field with no icon lost its name: " + line.getText(), line.getText().startsWith(TestRunEditorAttributes.RUN_STATUS.getName() + ": "));
    }

    // Rule-EDITOR-PANEL-270
    public void testHoveringTheDurationBadgeOnACardSaysDuration() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        final @NotNull RunItem runItem = new RunItem().setId(tc.getId()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).duration(Duration.ofSeconds(42)).build())).showing(Optional.of(tc), Optional.empty(), Optional.empty(), false);
        final @NotNull RunItemCard card = new RunItemCard(getProject());
        card.updateData(0, Set.of(TestRunEditorAttributes.DURATION), runItem, AbstractCard.titleText(1, true, tc.getDescription()));
        card.layOutAs(CELL);

        final @NotNull JBLabel badge = Drawn.first(card, JBLabel.class, label -> label.isVisible() && Display.formatDuration(Duration.ofSeconds(42)).equals(label.getText()));
        final @NotNull Rectangle at = SwingUtilities.convertRectangle(badge.getParent(), badge.getBounds(), card);

        assertEquals(TestRunEditorAttributes.DURATION.getName(), card.tooltipAt(new Point(at.x + at.width / 2, at.y + at.height / 2), CELL));
    }
}
