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

package org.testin.testcase.create;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.FilesUnder;
import org.testin.editor.EditorFixtures;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderByNumberIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestCaseDto stored(final @NotNull TestCaseDto tc) {
        return theTestCases().findTestCase(tc.getId()).orElseThrow();
    }

    private @NotNull TestCaseDto movedTo(final int position, final @NotNull TestCaseDto tc) {
        final @NotNull OrderSection order = new OrderSection(getProject());
        order.fillData(stored(tc));
        order.getPosition().setValue(position);
        final @NotNull TestCaseDto moved = order.applyTo(stored(tc));
        theTestCases().putTestCase(tc.getParent().getPath(), moved);
        return moved;
    }

    // Rule-EDITOR-PANEL-223
    public void testATestSetWithUnplacedTestCasesIsPlacedOnceSoATypedPositionMeansSomething() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode ts = EditorFixtures.testSet(getProject(), tp, "Imported");
        final @NotNull List<TestCaseDto> unplaced = new ArrayList<>();
        for (final String description : List.of("Log in", "Log out", "Pay by card", "Check out"))
            unplaced.add(EditorFixtures.testCase(getProject(), ts, description, ""));
        final @NotNull TestCaseDto last = TestCaseOrder.ordered(unplaced).getLast();

        final @NotNull TestCaseDto moved = movedTo(1, last);

        Await.until("the unplaced test cases were never given places", () -> unplaced.stream().filter(tc -> !tc.getId().equals(last.getId())).noneMatch(tc -> stored(tc).getOrder().isEmpty()));
        final @NotNull List<TestCaseDto> nowOrdered = TestCaseOrder.ordered(theTestCases().getTestCasesForTestSet(ts.getPath()));
        assertEquals("the typed position meant nothing among test cases that had no place", moved.getId(), nowOrdered.getFirst().getId());
        final @NotNull Map<String, String> placed = FilesUnder.snapshot(ts.getPath());

        movedTo(3, moved);

        assertEquals("the placing happened a second time", placed.keySet(), FilesUnder.snapshot(ts.getPath()).keySet());
        for (final TestCaseDto tc : unplaced) {
            if (tc.getId().equals(moved.getId())) continue;
            final @NotNull String file = placed.keySet().stream().filter(name -> name.contains(tc.getId().toString())).findFirst().orElseThrow();
            assertEquals("a test case around the one moved was written again", placed.get(file), FilesUnder.snapshot(ts.getPath()).get(file));
        }
    }
}
