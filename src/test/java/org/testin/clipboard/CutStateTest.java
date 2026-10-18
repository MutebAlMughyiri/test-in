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

package org.testin.clipboard;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.StandIn;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CutStateTest {

    private static @NotNull CutState aCutState() {
        return new CutState(StandIn.of(Project.class));
    }

    private static @NotNull TestCase testCase() {
        return TestCase.builder().uuid(UUID.randomUUID()).build();
    }

    @Test
    public void theTestCasesThatWereCutArePastedAsAMove() {
        final @NotNull TestCase first = testCase();
        final @NotNull TestCase second = testCase();
        final @NotNull CutState state = aCutState();

        state.cut(new TestSetNode(), List.of(first, second));

        assertTrue(state.isCutOf(List.of(first, second)));
    }

    @Test
    public void otherTestCasesCopiedAfterTheCutArePastedAsACopy() {
        final @NotNull CutState state = aCutState();

        state.cut(new TestSetNode(), List.of(testCase()));

        assertTrue(state.isCutting(), "the cut is still waiting");
        assertFalse(state.isCutOf(List.of(testCase())), "a different test case on the clipboard is not the cut, so pasting it moves nothing");
    }

    @Test
    public void aClipboardHoldingTheCutAndMoreIsNotTheCut() {
        final @NotNull TestCase cut = testCase();
        final @NotNull CutState state = aCutState();

        state.cut(new TestSetNode(), List.of(cut));

        assertFalse(state.isCutOf(List.of(cut, testCase())));
    }

    @Test
    public void nothingPastedIsNotAMove() {
        final @NotNull CutState state = aCutState();

        state.cut(new TestSetNode(), List.of(testCase()));

        assertFalse(state.isCutOf(List.of()));
    }

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-065
    @Test
    public void removingATestCaseThatIsNotWaitingLeavesTheCut() {
        final @NotNull TestCase cut = testCase();
        final @NotNull CutState state = aCutState();
        state.cut(new TestSetNode(), List.of(cut));

        state.callOffFor(List.of(testCase()));

        assertTrue(state.isCutOf(List.of(cut)), "removing an unrelated test case called off the cut, so the paste copied instead of moving");
    }
}
