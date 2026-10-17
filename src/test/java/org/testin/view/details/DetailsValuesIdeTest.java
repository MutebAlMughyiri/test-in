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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.editor.ShownFields;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.framework.ScreenshotImages;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DetailsValuesIdeTest extends AbstractViewPanelIdeTest {

    private static final @NotNull String ORDER = TestSetEditorFields.ORDER.getName().toUpperCase(Locale.ROOT);

    private static @NotNull TestCase everyFieldFilled(final @NotNull TestCase testCase) {
        testCase.setExpectedResult("The dashboard opens");
        testCase.setSteps(List.of("Open the login page", "Type the credentials"));
        testCase.setPreConditions("An account exists");
        testCase.setTestData("user=admin");
        testCase.setReference("JIRA-12");
        testCase.setModule("Accounts");
        return testCase;
    }

    private static @NotNull RunItem failedWith(final @NotNull TestCase testCase, final @NotNull List<String> screenshots) {
        return RunItem.builder().uuid(testCase.getUuid()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).actualResult("The session was dropped").duration(Duration.ofSeconds(134)).executedBy("muteb").stacktrace("java.lang.AssertionError: expected [true]").screenshots(screenshots).build())).build();
    }

    private static byte @NotNull [] aScreenshot(final int width, final int height) {
        return ScreenshotImages.toPng(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
    }

    private static @NotNull Container lineOf(final @NotNull Component part) {
        return part.getParent();
    }

    private static int heightOf(final Icon icon) {
        return Optional.ofNullable(icon).map(Icon::getIconHeight).orElse(0);
    }

    private static int widthOf(final Icon icon) {
        return Optional.ofNullable(icon).map(Icon::getIconWidth).orElse(0);
    }

    private @NotNull JBPanel<?> drawnWithEveryBandOpen(final @NotNull TestCase testCase, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> treePath) {
        final boolean wasOpen = PropertiesComponent.getInstance().getBoolean(DetailsTab.TEST_CASE_OPEN, false);
        PropertiesComponent.getInstance().setValue(DetailsTab.TEST_CASE_OPEN, true, false);
        try {
            return Drawn.detailsTab(getProject(), testCase, runItem, treePath);
        } finally {
            PropertiesComponent.getInstance().setValue(DetailsTab.TEST_CASE_OPEN, wasOpen, false);
        }
    }

    private @NotNull String orderIn(final @NotNull List<String> words) {
        final int at = words.indexOf(ORDER);
        assertTrue("no " + ORDER + " row was drawn: " + words, at >= 0 && at + 1 < words.size());
        return words.get(at + 1);
    }

    // Rule-VIEW-PANEL-028
    public void testNoValueCanBeTypedIntoOrTakesTheKeyboard() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        final @NotNull TestCase testCase = everyFieldFilled(aTestCase(testSet, "Log in with a valid user", "a"));
        final @NotNull TestRunNode testRun = aTestRun(List.of(failedWith(testCase, List.of())));
        final @NotNull JBPanel<?> tab = drawnWithEveryBandOpen(testCase, Services.getInstance(getProject(), TestRuns.class).findRunItems(testRun.getPath()).flatMap(runItems -> runItems.runItemOf(testCase.getUuid())), testRun.treePath());

        final @NotNull List<Component> drawn = Drawn.components(tab);
        assertTrue("the test case's own band was not drawn open: " + Drawn.words(tab), Drawn.holds(Drawn.words(tab), "The dashboard opens"));
        drawn.stream().filter(JTextComponent.class::isInstance).map(JTextComponent.class::cast)
                .forEach(value -> assertFalse("the value \"" + value.getText() + "\" can be typed into", value.isEditable()));

        tab.addNotify();
        try {
            final @NotNull KeyboardStop stop = new KeyboardStop();
            final @NotNull List<String> takers = drawn.stream().filter(stop::accept).map(component -> component.getClass().getSimpleName() + " \"" + Drawn.text(component) + "\"").toList();
            assertEquals("a value on the Details tab takes the keyboard from the tab", List.of(), takers);
        } finally {
            tab.removeNotify();
        }
    }

    // Rule-VIEW-PANEL-041
    public void testTheBreadcrumbIsOneStepForEachFolderAboveTheTestCase() {
        final @NotNull List<String> treePath = List.of("Demo", "Test Cases", "Accounts", "Login");
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), TestCase.builder().description("Log in with a valid user").build(), Optional.empty(), treePath);

        final @NotNull Container breadcrumb = lineOf(Drawn.reading(tab, "Demo"));
        final @NotNull List<JLabel> parts = Arrays.stream(breadcrumb.getComponents()).filter(JLabel.class::isInstance).map(JLabel.class::cast).toList();
        final @NotNull List<String> steps = parts.stream().map(part -> Objects.toString(part.getText(), "")).filter(text -> !text.isEmpty()).toList();
        final long arrows = parts.stream().filter(part -> Optional.ofNullable(part.getIcon()).isPresent() && Objects.toString(part.getText(), "").isEmpty()).count();

        assertEquals("the breadcrumb does not show one step for each folder, in order", treePath, steps);
        assertEquals("the steps are not joined by one arrow between each two", treePath.size() - 1, arrows);

        final @NotNull JBPanel<?> noPath = Drawn.detailsTab(getProject(), TestCase.builder().description("Log in with a valid user").build(), Optional.empty(), List.of());
        assertFalse("a panel handed no path drew a step: " + Drawn.words(noPath), Drawn.holds(Drawn.words(noPath), "Demo"));
    }

    // Rule-VIEW-PANEL-062
    public void testOrderIsWhereTheTestCaseSitsInItsTestSet() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull TestCase middle = aTestCase(testSet, "Log in with a locked user", "m");
        aTestCase(testSet, "Log in with no password", "z");

        assertEquals("Order was not read as the test case's place in its test set", "2", orderIn(Drawn.words(Drawn.detailsTab(getProject(), middle, Optional.empty(), testSet.treePath()))));

        aTestCase(testSet, "Log in twice", "b");
        assertEquals("Order did not follow the test set when a test case was put above it", "3", orderIn(Drawn.words(Drawn.detailsTab(getProject(), middle, Optional.empty(), testSet.treePath()))));
    }

    // Rule-VIEW-PANEL-062
    public void testATestCaseNoLongerInItsTestSetHasNoOrder() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull TestCase deleted = TestCase.builder().parent(testSet).description("Log in with a locked user").build();

        final @NotNull List<String> words = Drawn.words(Drawn.detailsTab(getProject(), deleted, Optional.empty(), testSet.treePath()));

        assertFalse("a test case its test set no longer holds was given a place in it: " + words, words.contains(ORDER));
    }

    // Rule-VIEW-PANEL-081
    public void testTheStacktraceLinkComesFirstThenOneThumbnailForEachScreenshot() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        final @NotNull TestCase testCase = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().uuid(testCase.getUuid()).build()));
        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        final @NotNull List<String> names = testRuns.storeScreenshots(testRun.getPath(), List.of(aScreenshot(320, 200), aScreenshot(640, 400)));
        assertEquals(2, names.size());

        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), testCase, Optional.of(failedWith(testCase, names)), testRun.treePath());
        final @NotNull Component link = Drawn.reading(tab, Bundle.message("view.stacktrace.link"));
        final @NotNull List<Component> line = Arrays.asList(lineOf(link).getComponents());

        assertEquals("the line does not hold the link and one thumbnail for each screenshot", 1 + names.size(), line.size());
        assertSame("the Stacktrace link is not first on its line", link, line.getFirst());
        assertTrue("the link is not a link", link instanceof ActionLink);
        for (int i = 0; i < names.size(); i++) {
            final @NotNull JBLabel thumbnail = (JBLabel) line.get(i + 1);
            final @NotNull String name = names.get(i);
            assertTrue("hovering a thumbnail does not name its file: " + thumbnail.getToolTipText(), String.valueOf(thumbnail.getToolTipText()).contains(name));
            Await.until("the thumbnail of " + name + " was never drawn 48 pixels high", () -> heightOf(thumbnail.getIcon()) == 48 && widthOf(thumbnail.getIcon()) > 48);
        }

        final @NotNull JBLabel second = (JBLabel) line.get(2);
        final @NotNull MouseEvent click = new MouseEvent(second, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : second.getMouseListeners()) listener.mouseClicked(click);
        try {
            final @NotNull JComponent shown = ShownDialog.content(getProject(), ScreenshotDialog.class);
            final @NotNull Icon picture = Drawn.components(shown).stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast).map(JBLabel::getIcon).filter(icon -> widthOf(icon) > 48).findFirst()
                    .orElseThrow(() -> new AssertionError("the screenshot's window shows no picture"));
            assertEquals("the screenshot did not open at its real width", 640, picture.getIconWidth());
            assertEquals("the screenshot did not open at its real height", 400, picture.getIconHeight());
        } finally {
            ShownDialog.close(getProject(), ScreenshotDialog.class);
        }
    }

    // Rule-VIEW-PANEL-103, Rule-VIEW-PANEL-081
    public void testTheStacktraceAndScreenshotsAreDrawnWhenFieldsHidesTheStacktrace() {
        final @NotNull TestSetNode testSet = aTestSet("Login");
        final @NotNull TestCase testCase = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull TestRunNode testRun = aTestRun(List.of(RunItem.builder().uuid(testCase.getUuid()).build()));
        final @NotNull List<String> names = Services.getInstance(getProject(), TestRuns.class).storeScreenshots(testRun.getPath(), List.of(aScreenshot(320, 200)));
        final @NotNull Set<TestRunEditorFields> was = ShownFields.read(ShownFields.IN_TEST_RUNS, TestRunEditorFields.class);
        final @NotNull Set<TestRunEditorFields> chosen = EnumSet.allOf(TestRunEditorFields.class);
        chosen.remove(TestRunEditorFields.STACKTRACE);
        ShownFields.write(ShownFields.IN_TEST_RUNS, chosen);
        try {
            final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), testCase, Optional.of(failedWith(testCase, names)), testRun.treePath());

            assertTrue("the Stacktrace link went missing because Fields hides the Stacktrace", Drawn.words(tab).contains(Bundle.message("view.stacktrace.link")));
            assertTrue("the screenshot was not drawn because Fields hides the Stacktrace", Drawn.components(tab).stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast).anyMatch(label -> Drawn.hovering(label).contains(names.getFirst())));
        } finally {
            ShownFields.write(ShownFields.IN_TEST_RUNS, was);
        }
    }

    private static final class KeyboardStop extends LayoutFocusTraversalPolicy {
        @Override
        public boolean accept(final @NotNull Component component) {
            return super.accept(component);
        }
    }
}
