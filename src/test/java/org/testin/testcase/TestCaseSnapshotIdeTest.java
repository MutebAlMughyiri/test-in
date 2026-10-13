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

package org.testin.testcase;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class TestCaseSnapshotIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestSetNode checkoutSet() {
        return WriteAction.computeAndWait(() -> {
            final NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);

            final TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(testProject);

            final TestSetNode testSet = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Checkout"), testProject.getTestCasesFolder());
            nodes().addTestSet(testSet);
            return testSet;
        });
    }

    // Rule-EDITOR-PANEL-255
    public void testUndoingAPasteWhoseSourceSetIsGoneLeavesTheTestCasesInTheDestination() {
        final TestSetNode destination = checkoutSet();

        final Path source = destination.getPath().resolveSibling("Login");

        final TestCaseDto moved = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order("m")
                .build();
        moved.setParent(destination);
        final List<UUID> ids = List.of(moved.getId());

        final TestCaseSnapshot sourceBefore = new TestCaseSnapshot(source, List.of(moved), List.of());
        final TestCaseSnapshot destinationBefore = TestCaseSnapshot.of(getProject(), destination.getPath(), ids);

        indexedTestCases().putTestCaseVerbatim(destination.getPath(), moved);

        final UndoScope scope = UndoScope.of(destination.getPath());
        TestCaseSnapshot.record(getProject(), scope, "Paste",
                List.of(sourceBefore, destinationBefore),
                List.of(TestCaseSnapshot.of(getProject(), source, ids), TestCaseSnapshot.of(getProject(), destination.getPath(), ids)));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertFalse("an undo into a set no longer indexed was not refused",
                Services.getInstance(getProject(), UndoHistories.class).undo(scope));
        assertTrue("the pasted test case was taken out of the destination by a refused undo",
                indexedTestCases().findTestCase(moved.getId()).isPresent());
    }

    public void testUndoingARemovalPutsTheTestCaseBack() {
        final TestSetNode testSet = checkoutSet();
        final TestCaseDto removed = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Pay with a saved card")
                .order("m")
                .build();
        removed.setParent(testSet);
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), removed);
        final List<UUID> ids = List.of(removed.getId());

        final TestCaseSnapshot before = TestCaseSnapshot.of(getProject(), testSet.getPath(), ids);
        assertTrue("the removal being undone did not happen", indexedTestCases().removeTestCase(testSet.getPath(), removed.getId()));

        final UndoScope scope = UndoScope.of(testSet.getPath());
        TestCaseSnapshot.record(getProject(), scope, "Remove", List.of(before), List.of(TestCaseSnapshot.of(getProject(), testSet.getPath(), ids)));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the undo said it could not put the test case back", Services.getInstance(getProject(), UndoHistories.class).undo(scope));
        assertTrue("the test case is not back in the index", indexedTestCases().findTestCase(removed.getId()).isPresent());
        assertTrue("the test case's file is not back on disk", Files.isRegularFile(testSet.getPath().resolve(removed.getId() + ".tc")));
    }
}
