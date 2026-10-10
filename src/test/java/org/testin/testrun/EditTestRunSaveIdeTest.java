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

package org.testin.testrun;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EditTestRunSaveIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull UUID JUDGED_WHILE_OPEN = UUID.fromString("22222222-2222-4222-8222-222222222201");
    private static final @NotNull UUID STILL_PENDING = UUID.fromString("22222222-2222-4222-8222-222222222202");
    private static final @NotNull UUID ADDED_BY_THE_EDIT = UUID.fromString("22222222-2222-4222-8222-222222222203");

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull TestRunNode anOpenTestRun() {
        final @NotNull TestRunNode testRun = WriteAction.computeAndWait(() -> {
            final @NotNull NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            final @NotNull TestProjectNode tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(tp);

            final @NotNull TestRunNode tr = mapper.setTestRunNode(tp.getTestRunsFolder().getPath().resolve("Cycle-1"), tp.getTestRunsFolder());
            nodes.addTestRunNode(tr);
            return tr;
        });

        indexedTestRuns().putRunItems(testRun.getPath(), new RunItems().setAll(List.of(
                new RunItem().setId(JUDGED_WHILE_OPEN).setStatus(RunItemStatus.PENDING),
                new RunItem().setId(STILL_PENDING).setStatus(RunItemStatus.PENDING))));
        return testRun;
    }

    private @NotNull RunItemStatus statusOf(final @NotNull TestRunNode testRun, final @NotNull UUID testCaseId) {
        return indexedTestRuns().getRunItems(testRun.getPath()).runItemOf(testCaseId).map(RunItem::getStatus).orElse(RunItemStatus.REMOVED);
    }

    private boolean save(final @NotNull TestRunNode testRun) {
        final @NotNull Set<UUID> chosen = Set.of(JUDGED_WHILE_OPEN, STILL_PENDING, ADDED_BY_THE_EDIT);
        final boolean saved = new EditTestRunWork(getProject()).saveEdit(testRun, testRun.getName(), chosen, chosen, Map.of());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return saved;
    }

    // Rule-TREE-PANEL-128, Rule-TREE-PANEL-074
    public void testSavingKeepsARunItemStatusThatArrivedWhileTheDialogWasOpen() {
        final @NotNull TestRunNode testRun = anOpenTestRun();

        indexedTestRuns().changeRunItems(testRun.getPath(), held -> held.runItemOf(JUDGED_WHILE_OPEN).ifPresent(result -> result.setStatus(RunItemStatus.FAILED)));

        assertTrue("the edit was refused", save(testRun));
        assertEquals("the run item status that arrived while the dialog was open was replaced", RunItemStatus.FAILED, statusOf(testRun, JUDGED_WHILE_OPEN));
        assertEquals(RunItemStatus.PENDING, statusOf(testRun, STILL_PENDING));
        assertEquals("a test case the edit added is not Pending", RunItemStatus.PENDING, statusOf(testRun, ADDED_BY_THE_EDIT));
    }

    private void refusedOnceSignedOff(final @NotNull TestRunStatus status) {
        final @NotNull TestRunNode testRun = anOpenTestRun();
        testRun.getMarker().changeStatus(status);

        assertFalse("a " + status + " test run took an edit", save(testRun));
        assertEquals("a " + status + " test run changed what it covers", Set.of(JUDGED_WHILE_OPEN, STILL_PENDING), indexedTestRuns().getRunItems(testRun.getPath()).coveredIds());
    }

    // Rule-TREE-PANEL-009, Rule-TREE-PANEL-073
    public void testACompletedTestRunRefusesTheEdit() {
        refusedOnceSignedOff(TestRunStatus.COMPLETED);
    }

    // Rule-TREE-PANEL-009, Rule-TREE-PANEL-073
    public void testAClosedTestRunRefusesTheEdit() {
        refusedOnceSignedOff(TestRunStatus.CLOSED);
    }

    // Rule-TREE-PANEL-073, Rule-TREE-PANEL-135
    public void testACommittedTestRunRefusesTheEdit() {
        refusedOnceSignedOff(TestRunStatus.COMMITTED);
    }

    // Rule-TREE-PANEL-060
    public void testASavedEditCannotBeUndone() {
        final @NotNull TestRunNode testRun = anOpenTestRun();

        assertTrue("the edit was refused", save(testRun));
        assertFalse("a saved edit of a test run went onto the tree's undo history",
                Services.getInstance(getProject(), UndoHistories.class).canUndo(UndoScope.TREE));
    }
}
