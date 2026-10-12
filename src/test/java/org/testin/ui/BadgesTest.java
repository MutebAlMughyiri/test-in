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

package org.testin.ui;

import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Display;
import org.testng.annotations.Test;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class BadgesTest {

    private static List<Badge> bugBadges(final RunItem runItem) {
        final List<Badge> badges = new ArrayList<>();

        TestRunEditorAttributes.BUG_SEVERITY.applyToUI(runItem, badges, new HashMap<>());
        TestRunEditorAttributes.BUG_PRIORITY.applyToUI(runItem, badges, new HashMap<>());

        return badges;
    }

    @Test
    public void aLightBackgroundEarnsDarkText() {
        assertTrue(Badges.isLight(Color.YELLOW), "yellow with white text is the failure this exists to stop");
        assertTrue(Badges.isLight(Color.ORANGE), "orange is light enough to lose white text");
        assertTrue(Badges.isLight(Color.GREEN), "pure green is brighter than it looks");
        assertTrue(Badges.isLight(Color.GRAY.brighter()), "the test case's Low priority, marginal under white");
    }

    @Test
    public void aDarkBackgroundKeepsWhiteText() {
        assertFalse(Badges.isLight(Color.RED), "a blocker is deep red");
        assertFalse(Badges.isLight(Color.BLUE), "blue is dark however bright the channel is");
        assertFalse(Badges.isLight(Color.DARK_GRAY), "the group badge");
    }

    @Test
    public void brightnessIsWhatTheEyeSeesNotWhatTheChannelSays() {
        assertTrue(Badges.isLight(Color.GREEN));
        assertFalse(Badges.isLight(Color.BLUE));
    }

    // Rule-EDITOR-PANEL-253
    @Test
    public void aTestCaseThatNeverFailedDrawsNoPill() {
        final List<Badge> badges = bugBadges(RunItem.builder().executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build());

        assertEquals(badges.size(), 0, "a severity is shown only on a failure, so a pass draws no badge");
    }

    // Rule-EDITOR-PANEL-147
    @Test
    public void aFailureTheTesterDidNotTriageDrawsItsDefaults() {
        final List<Badge> badges = bugBadges(RunItem.builder().executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build());

        assertEquals(badges.size(), 1);
        assertTrue(badges.getFirst() instanceof BugPill bug && bug.text().equals("Enhancement / Low"),
                "a severity and a priority nobody chose are Enhancement and Low");
    }

    // Rule-EDITOR-PANEL-253
    @Test
    public void oneHalfOnItsOwnIsThatHalf() {
        final List<Badge> severityOnly = new ArrayList<>();
        Badges.addBugBadge(severityOnly, "Bug Severity", BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        assertEquals(severityOnly.size(), 1);
        assertTrue(severityOnly.getFirst() instanceof BugPill bug && bug.text().equals("Major"));

        final List<Badge> priorityOnly = new ArrayList<>();
        Badges.addBugBadge(priorityOnly, "Bug Priority", BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());
        assertEquals(priorityOnly.size(), 1);
        assertTrue(priorityOnly.getFirst() instanceof BugPill bug && bug.text().equals("High"),
                "the survivor keeps its own color, which is why BugPriority still declares one");
    }

    // Rule-EDITOR-PANEL-253
    @Test
    public void bothHalvesJoinIntoOneBadge() {
        final List<Badge> badges = new ArrayList<>();

        Badges.addBugBadge(badges, "Bug Severity", BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        Badges.addBugBadge(badges, "Bug Priority", BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());

        assertEquals(badges.size(), 1, "two halves, one badge");
        assertTrue(badges.getFirst() instanceof BugPill bug && bug.text().equals("Major / High"),
                "severity first, because the enum offers it first");
        assertEquals(((BugPill) badges.getFirst()).color(), BugSeverity.MAJOR.getColor(),
                "the color is the first half's");
    }

    // Rule-EDITOR-PANEL-253, Rule-VIEW-PANEL-086
    @Test
    public void hoveringTheBugBadgeNamesEachHalf() {
        final List<Badge> both = new ArrayList<>();
        Badges.addBugBadge(both, "Bug Severity", BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        Badges.addBugBadge(both, "Bug Priority", BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());
        assertEquals(both.getFirst().tooltip(), "Bug Severity: Major, Bug Priority: High");

        final List<Badge> priorityOnly = new ArrayList<>();
        Badges.addBugBadge(priorityOnly, "Bug Priority", BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());
        assertEquals(priorityOnly.getFirst().tooltip(), "Bug Priority: High", "one half on its own names that half");
    }

    // Rule-VIEW-PANEL-114, Rule-EDITOR-PANEL-268
    @Test
    public void thePriorityBadgeNamesItsFieldOnHover() {
        final List<Badge> badges = new ArrayList<>();

        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.HIGH));

        assertEquals(badges.getFirst().text(), "High", "the badge does not say the priority's word");
        assertTrue(badges.getFirst() instanceof Pill pill && pill.tooltip().equals("Priority: High"), "hovering the badge does not name the field, so it reads like the bug's priority");
    }

    // Rule-EDITOR-PANEL-267
    @Test
    public void anEditorsCardDrawsThePriorityAsNeitherABadgeNorALine() {
        final TestCaseDto tc = new TestCaseDto().setPriority(Priority.HIGH);
        final List<Badge> badges = new ArrayList<>();
        final Map<String, String> details = new HashMap<>();

        TestSetEditorAttributes.PRIORITY.applyToUI(tc, badges, details);
        TestRunEditorAttributes.PRIORITY.applyToUI(RunItem.pendingFor(tc), badges, details);

        assertTrue(badges.isEmpty(), "an editor's card still draws the priority as a badge");
        assertTrue(details.isEmpty(), "an editor's card draws the priority as a detail line");
    }

    @Test
    public void theDefaultPriorityDrawsNoPill() {
        final List<Badge> badges = new ArrayList<>();

        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.DEFAULT));
        assertEquals(badges.size(), 0, "the default priority needs no badge");

        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.HIGH));
        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.MEDIUM));
        assertEquals(badges.size(), 2, "a priority somebody chose is still drawn");
    }

    @Test
    public void theThreePrioritiesAreThreeColours() {
        final Set<Color> colors = new HashSet<>();

        for (final Priority priority : Priority.values()) {
            assertTrue(colors.add(new Color(priority.getColor().getRGB())), priority + " repeats another priority's color");
        }

        assertEquals(colors.size(), 3, "three priorities, three colors, no caption to fall back on");
    }

    @Test
    public void everySeverityThatDrawsHasItsOwnNameAndColour() {
        final Set<String> names = new HashSet<>();
        final Set<Color> colors = new HashSet<>();

        for (final BugSeverity severity : BugSeverity.values()) {
            assertFalse(severity.getLabel().isBlank(), severity + " is drawn, so it needs a name");
            assertTrue(names.add(severity.getLabel()), severity + " repeats another severity's name");
            assertTrue(colors.add(new Color(severity.getColor().getRGB())), severity + " repeats another severity's color");
        }

        assertEquals(names.size(), 4, "the four severities a tester can choose");
    }

    // Rule-EDITOR-PANEL-252
    @Test
    public void aPaleBadgeNeverCarriesWhiteText() {
        assertNotEquals(Badges.readableOn(Color.YELLOW).getRGB(), Color.WHITE.getRGB(), "white on yellow cannot be read");
        assertNotEquals(Badges.readableOn(Color.GREEN).getRGB(), Color.WHITE.getRGB(), "nor white on bright green");
        assertEquals(Badges.readableOn(Color.RED).getRGB(), Color.WHITE.getRGB(), "a deep red badge keeps its white words");
        assertEquals(Badges.readableOn(Color.BLUE).getRGB(), Color.WHITE.getRGB());
    }

    // Rule-EDITOR-PANEL-270
    @Test
    public void aRunItemsDurationIsABadgeOnItsCardAndNeverALine() {
        final List<Badge> badges = new ArrayList<>();
        final Map<String, String> details = new HashMap<>();

        TestRunEditorAttributes.DURATION.applyToUI(RunItem.builder().executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).duration(Duration.ofSeconds(42)).build())).build(), badges, details);

        assertEquals(badges.size(), 1, "the duration is not one badge");
        assertTrue(badges.getFirst() instanceof Framed framed && framed.text().equals(Display.formatDuration(Duration.ofSeconds(42))), "the badge does not show the duration");
        assertTrue(details.isEmpty(), "the duration is still drawn as a line");
        assertEquals(badges.getFirst().tooltip(), TestRunEditorAttributes.DURATION.getName(), "hovering the duration badge does not say Duration");
    }

    // Rule-EDITOR-PANEL-270
    @Test
    public void aRunItemNotRunYetHasNoDurationBadge() {
        final List<Badge> badges = new ArrayList<>();

        TestRunEditorAttributes.DURATION.applyToUI(RunItem.builder().executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build(), badges, new HashMap<>());

        assertTrue(badges.isEmpty(), "a run item with no duration drew a badge");
    }
}
