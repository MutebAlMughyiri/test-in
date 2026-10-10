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
package org.testin.view.details;

import com.intellij.icons.AllIcons;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import static org.testin.view.Drawn.holds;

public class DetailsBandsIdeTest extends BasePlatformTestCase {

    private static final @NotNull String RUN = Bundle.message("details.band.run").toUpperCase(Locale.ROOT);
    private static final @NotNull String TEST_CASE = Bundle.message("details.band.test.case").toUpperCase(Locale.ROOT);
    private static final @NotNull UUID ID = UUID.fromString("3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b");
    private static final @NotNull String LAST_STEP = "Login";
    private static final @NotNull String STACKTRACE = """
            java.lang.AssertionError: expected [true]
                at one
                at two
                at three
                at four
                at five""";

    private static @NotNull TestCaseDto aTestCase() {
        return TestCaseDto.builder().id(ID).description("Log in with a valid user").expectedResult("The dashboard opens").module("Accounts").build();
    }

    private static @NotNull RunItem failed() {
        return RunItem.builder().id(ID).status(RunItemStatus.FAILED).actualResult("The session was dropped").duration(Duration.ofSeconds(134)).executedBy("muteb").stacktrace(STACKTRACE).build();
    }

    private @NotNull List<String> shown(final @NotNull Optional<RunItem> runItem) {
        return Drawn.words(Drawn.detailsTab(getProject(), aTestCase(), runItem, List.of("Demo", "Test Cases", LAST_STEP)));
    }

    // Rule-VIEW-PANEL-034
    public void testTheStacktraceIsALinkAndNotAValue() {
        final @NotNull List<String> words = shown(Optional.of(failed()));

        assertTrue("the link that opens the stacktrace is missing: " + words, words.contains(Bundle.message("view.stacktrace.link")));
        assertFalse("the stacktrace itself was drawn on the panel: " + words, holds(words, "java.lang.AssertionError"));
    }

    // Rule-VIEW-PANEL-085
    public void testTheTestRunBandComesBeforeTheTestCaseBand() {
        final @NotNull List<String> words = shown(Optional.of(failed()));

        assertTrue("the test run band was not drawn for a test case the test run holds: " + words, words.contains(RUN));
        assertTrue("the test case band was not drawn: " + words, words.contains(TEST_CASE));
        assertTrue("the test case band was drawn above the test run band", words.indexOf(RUN) < words.indexOf(TEST_CASE));
    }

    // Rule-VIEW-PANEL-085
    public void testATestCaseWithNoTestRunIsDrawnWithNoBandsAtAll() {
        final @NotNull List<String> words = shown(Optional.empty());

        assertFalse("the test run band was drawn with nothing to put in it: " + words, words.contains(RUN));
        assertFalse("a band's name was drawn over the only thing on the panel: " + words, words.contains(TEST_CASE));
        assertTrue("the test case's fields were not drawn: " + words, holds(words, "The dashboard opens"));
    }

    public void testThePanelShowsNoTestCaseId() {
        assertFalse("the panel is still drawing the id", shown(Optional.empty()).contains(ID.toString()));
    }

    // Rule-VIEW-PANEL-087
    public void testTheTestCaseBandIsFoldedWhereATestRunStandsAboveIt() {
        final @NotNull List<String> words = shown(Optional.of(failed()));

        assertTrue("the test case band's name was not drawn: " + words, words.contains(TEST_CASE));
        assertFalse("a test case field was drawn while the band was folded: " + words, holds(words, "The dashboard opens"));
        assertFalse("a test case field was drawn while the band was folded: " + words, holds(words, "Accounts"));
    }

    // Rule-INTERNAL-133
    public void testTheFoldedBandDrawsTheAccentChevron() {
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), aTestCase(), Optional.of(failed()), List.of("Demo", "Test Cases", LAST_STEP));
        final @NotNull JLabel caption = Drawn.first(tab, JLabel.class, label -> TEST_CASE.equalsIgnoreCase(label.getText()));

        final @NotNull Icon chevron = Objects.requireNonNull(caption.getIcon());
        assertNotSame("the band draws the platform's uncolored arrow", AllIcons.General.ArrowRight, chevron);
        assertEquals("the band's chevron is not the one every collapsible section draws", DialogStyle.chevron(false).getClass(), chevron.getClass());
        assertTrue("the folded band does not say Expand at the end of its line", Drawn.words(tab).contains(Bundle.message("panel.expand")));
        assertEquals("hovering the folded band's heading does not say a click expands it", Bundle.message("panel.expand.tooltip"), Drawn.hovering((JComponent) caption.getParent()));
    }
}
