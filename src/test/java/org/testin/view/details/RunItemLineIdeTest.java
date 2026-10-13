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

import com.intellij.openapi.application.WriteAction;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestProjectNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class RunItemLineIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull UUID TEST_CASE_ID = UUID.fromString("55555555-5555-4555-8555-555555555501");
    private static final @NotNull String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private static @NotNull TestCaseDto aTestCase() {
        return TestCaseDto.builder().id(TEST_CASE_ID).description("Log in with a valid user").expectedResult("The dashboard opens").build();
    }

    private static @NotNull RunItem failed(final @NotNull String bugIssueUrl) {
        return RunItem.builder().id(TEST_CASE_ID).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl(bugIssueUrl).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).duration(Duration.ofSeconds(134)).stacktrace("java.lang.AssertionError: expected [true]").build())).build();
    }

    private static @NotNull Component rowOf(final @NotNull Component drawn) {
        @NotNull Component row = drawn;
        while (!(row.getParent().getLayout() instanceof GridBagLayout)) row = row.getParent();
        return row;
    }

    private @NotNull List<String> shownTestRunPath() {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            final @NotNull TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(testProject);

            final @NotNull Path path = testProject.getTestRunsFolder().getPath().resolve("Cycle-1");
            nodes.addTestRunNode(mapper.setTestRunNode(path, testProject.getTestRunsFolder()));
            return path;
        });
        return List.of(testRunPath.toString());
    }

    private @NotNull JBPanel<?> drawn(final @NotNull RunItem runItem, final @NotNull List<String> testRunPath) {
        return Drawn.detailsTab(getProject(), aTestCase(), Optional.of(runItem), testRunPath);
    }

    // Rule-VIEW-PANEL-086
    public void testRunItemStatusDurationAndBugChipAreOneLine() {
        final @NotNull JBPanel<?> tab = drawn(failed(""), shownTestRunPath());
        final @NotNull Component status = Drawn.reading(tab, RunItemStatus.FAILED.getLabel());
        final @NotNull Component duration = Drawn.reading(tab, "02:14");
        final @NotNull Component bugChip = Drawn.components(tab).stream()
                .filter(component -> Drawn.text(component).startsWith(BugSeverity.MAJOR.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the bug chip was not drawn: " + Drawn.words(tab)));

        assertSame("the duration is not on the run item status's line", rowOf(status), rowOf(duration));
        assertSame("the bug chip is not on the run item status's line", rowOf(status), rowOf(bugChip));
        assertTrue("the bug chip does not carry the bug priority: " + Drawn.text(bugChip), Drawn.text(bugChip).contains(BugPriority.HIGH.getLabel()));
    }

    // Rule-VIEW-PANEL-033
    public void testATestCaseTheTestRunHasNotReachedReadsPending() {
        final @NotNull List<String> words = Drawn.words(drawn(RunItem.builder().id(TEST_CASE_ID).build(), shownTestRunPath()));

        assertTrue("an unreached test case does not read Pending: " + words, words.contains(Bundle.message("status.run.item.pending")));
    }

    // Rule-VIEW-PANEL-031
    public void testAnEmptyTestRunValueIsNotDrawn() {
        final @NotNull List<String> words = Drawn.words(drawn(RunItem.builder().id(TEST_CASE_ID).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build(), shownTestRunPath()));

        assertFalse("an empty actual result was drawn: " + words, words.contains(Bundle.message("attribute.run.item.actual.result").toUpperCase(Locale.ROOT)));
        assertFalse("an empty executed by was drawn: " + words, words.contains(Bundle.message("attribute.run.item.executed.by").toUpperCase(Locale.ROOT)));
        assertFalse("a passed test case with no bug was offered a bug: " + words, words.contains(Bundle.message("bug.dialog.title")));
        assertFalse("a test case with no stacktrace was offered one: " + words, words.contains(Bundle.message("view.stacktrace.link")));
    }

    // Rule-VIEW-PANEL-080
    public void testNoLinkOnTheDetailsTabTakesTheKeyboard() {
        final @NotNull List<String> testRunPath = shownTestRunPath();
        final @NotNull List<ActionLink> links = Stream.of(drawn(failed(""), testRunPath), drawn(failed(ISSUE), testRunPath))
                .flatMap(tab -> Drawn.components(tab).stream())
                .filter(ActionLink.class::isInstance)
                .map(ActionLink.class::cast)
                .toList();

        assertEquals("the stacktrace, Report Bug and issue links were not all drawn",
                Stream.of(Bundle.message("view.stacktrace.link"), Bundle.message("bug.dialog.title"), Bundle.message("view.stacktrace.link"), "#412").sorted().toList(),
                links.stream().map(ActionLink::getText).sorted().toList());
        links.forEach(link -> assertFalse("the link " + link.getText() + " takes the keyboard", link.isFocusable()));
    }
}
