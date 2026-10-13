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

package org.testin.view.history;

import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.git.change.ChangeType;
import org.testin.git.change.FieldChange;
import org.testin.git.history.TestCaseHistoryCard;
import org.testin.git.history.TestCaseHistoryKind;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class HistoryTabIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull JBPanel<?> tab = new JBPanel<>(new BorderLayout());

    private static @NotNull RunItem failed(final @NotNull TestCaseDto tc) {
        return RunItem.builder().id(tc.getId()).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl("https://github.com/mtb550/test-in/issues/412").executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The basket was emptied").build())).build();
    }

    private @NotNull TestCaseDto aTestCase() {
        return EditorFixtures.testCase(getProject(), EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Login"), "Log in with a valid user", "a");
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-109
    public void testABugIsACardOfItsOwnWithItsAttributesAndItsTestRunOpensTheRunItem() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Login");
        final @NotNull TestCaseDto tc = EditorFixtures.testCase(getProject(), testSet, "Log in with a valid user", "a");
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, List.of(failed(tc)));
        Services.getInstance(getProject(), TestRuns.class).putRunItems(testProject.getTestRunsFolder().getPath().resolve("Cycle-0"), RunItems.builder().all(new ArrayList<>(List.of(failed(tc)))).build());

        new HistoryTab().load(getProject(), tab, Optional.of(tc));
        Await.until("the bug cards were never drawn: " + Drawn.words(tab), () -> Drawn.holds(Drawn.words(tab), Bundle.message("view.history.not.under.git")));

        final @NotNull List<String> words = Drawn.words(tab);
        for (final String shown : List.of(Bundle.message("view.history.bug"), BugSeverity.MAJOR.getLabel(), BugPriority.HIGH.getLabel(), "#412")) {
            assertTrue("a bug card does not show " + shown + ": " + words, Drawn.holds(words, shown));
        }
        assertFalse("a bug card shows the actual result: " + words, Drawn.holds(words, "The basket was emptied"));
        assertEquals("one bug card per test run, then the line: " + words, 2, words.stream().filter(Bundle.message("view.history.bug")::equals).count());

        final @NotNull JBLabel gone = (JBLabel) Drawn.reading(tab, "Cycle-0");
        assertTrue("a test run no longer in the test project does not say so: " + gone.getToolTipText(), String.valueOf(gone.getToolTipText()).contains(Bundle.message("view.history.bug.test.run.gone")));

        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        try {
            ((ActionLink) Drawn.reading(tab, testRun.getName())).doClick();
            Await.until("the test run did not open", () -> editors.editorFor(testRun).filter(TestRunEditor.class::isInstance).isPresent());
            final @NotNull TestRunEditor opened = (TestRunEditor) editors.editorFor(testRun).orElseThrow();
            Await.until("the run item was not selected in its test run", () -> Optional.ofNullable(opened.getList().getSelectedValue()).map(TestCaseDto::getId).filter(tc.getId()::equals).isPresent());
        } finally {
            editors.closeAll();
        }
    }

    private static @NotNull TestCaseHistoryCard updated(final int fields, final @NotNull String newValue) {
        final @NotNull List<FieldChange> changes = IntStream.range(0, fields)
                .mapToObj(i -> new FieldChange("Field " + i, "was " + i, i == 0 ? newValue : "now " + i, ChangeType.CHANGE_DESCRIPTION))
                .toList();
        return new TestCaseHistoryCard(TestCaseHistoryKind.UPDATED, "", "Sara", ZonedDateTime.parse("2026-10-07T16:40:00+03:00"), "", "", changes, "before", "after");
    }

    // Rule-VIEW-PANEL-116
    public void testATestCaseCardShowsTwoChangedFieldsAndHowManyMore() {
        final @NotNull List<String> five = Drawn.words(HistoryCardPanel.of(getProject(), updated(5, "now 0"), aTestCase(), 0));
        assertTrue("five changed fields do not end with +3 more: " + five, Drawn.holds(five, Bundle.message("view.history.more", "3")));
        assertFalse("a third changed field was drawn: " + five, Drawn.holds(five, "was 2"));

        final @NotNull List<String> three = Drawn.words(HistoryCardPanel.of(getProject(), updated(3, "now 0"), aTestCase(), 0));
        assertTrue("three changed fields do not end with +1 more: " + three, Drawn.holds(three, Bundle.message("view.history.more", "1")));

        final @NotNull List<String> two = Drawn.words(HistoryCardPanel.of(getProject(), updated(2, "now 0"), aTestCase(), 0));
        assertFalse("two changed fields say there are more: " + two, Drawn.holds(two, "more"));
    }

    // Rule-VIEW-PANEL-116
    public void testALongValueIsCutToThePanelAndWholeOnHover() {
        final @NotNull String longValue = "The transfer is sent and the balance drops by the amount plus the fee. ".repeat(12).trim();
        final @NotNull JComponent card = HistoryCardPanel.of(getProject(), updated(1, longValue), aTestCase(), 0);
        final @NotNull ScrollablePanel tabPanel = new ScrollablePanel(new BorderLayout());
        tabPanel.add(card, BorderLayout.NORTH);
        final @NotNull JBScrollPane pane = new JBScrollPane(tabPanel);
        pane.setSize(JBUI.scale(300), JBUI.scale(400));
        pane.validate();

        assertTrue("the card is wider than the panel: " + card.getWidth() + " > " + pane.getViewport().getWidth(), card.getWidth() <= pane.getViewport().getWidth());
        final @NotNull JBLabel line = Drawn.first(card, JBLabel.class, label -> String.valueOf(label.getText()).contains("The transfer"));
        assertEquals("a cut line is not whole on hover", "was 0 → " + longValue, Drawn.hovering(line));
    }

    // UC-VIEW-PANEL-007
    public void testNoTestCaseShownDrawsNothing() {
        new HistoryTab().load(getProject(), tab, Optional.empty());

        assertEquals(List.of(), Drawn.words(tab));
    }

    // Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-100
    public void testATestProjectNotUnderGitSaysSoInOneLineWithNoReadingLineFirst() {
        new HistoryTab().load(getProject(), tab, Optional.of(aTestCase()));

        assertEquals("a read that has not yet taken 0.3 seconds shows a Reading line", List.of(), Drawn.words(tab));
        Await.until("the History tab never said the test project is not under Git: " + Drawn.words(tab),
                () -> Drawn.words(tab).equals(List.of(Bundle.message("view.history.not.under.git"))));
    }

    // Rule-VIEW-PANEL-099
    public void testAnAnswerForATestCaseNoLongerShownIsNeverDrawn() {
        final @NotNull HistoryTab history = new HistoryTab();
        history.load(getProject(), tab, Optional.of(aTestCase()));
        history.load(getProject(), tab, Optional.empty());

        for (int turn = 0; turn < 25; turn++) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }

        assertEquals("the history of a test case paged away from was drawn", List.of(), Drawn.words(tab));
    }
}
