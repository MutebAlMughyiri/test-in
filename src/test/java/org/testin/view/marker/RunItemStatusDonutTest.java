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

package org.testin.view.marker;

import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeFigureRow;
import org.testin.model.NodeFigures;
import org.testin.model.DonutSlices;
import org.testin.model.testrun.TestRunSummary;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RunItemStatusDonutTest {

    private static final List<NodeFigureRow> SLICES = DonutSlices.RUN_ITEM_STATUSES.getSlices();

    private static @NotNull NodeFigures testRunFigures(final long passed, final long failed, final long blocked, final long untested) {
        final long total = passed + failed + blocked + untested;

        return NodeFigures.ofTestRun(new TestRunSummary(total, passed, failed, blocked, untested, 0, ""));
    }

    private static double sum(final double[] sweeps) {
        return Arrays.stream(sweeps).sum();
    }

    // Rule-TREE-PANEL-126
    @Test
    public void oneFailureInFiveHundredIsStillVisible() {
        final double[] sweeps = RunItemStatusDonut.sweeps(SLICES, testRunFigures(499, 1, 0, 0));

        assertTrue(sweeps[1] > 2.0, "a failure somebody recorded has to be visible: " + sweeps[1]);
    }

    @Test
    public void theRingStillClosesWhenASliverWasEnlarged() {
        final double[] sweeps = RunItemStatusDonut.sweeps(SLICES, testRunFigures(497, 1, 1, 1));

        assertEquals(sum(sweeps), 360.0, 0.0001, "the arcs must still add up to a circle");
    }

    @Test
    public void aRunItemStatusWithNoTestCasesTakesNoneOfTheRing() {
        final double[] sweeps = RunItemStatusDonut.sweeps(SLICES, testRunFigures(420, 60, 40, 30));

        assertEquals(sweeps[4], 0.0, 0.0, "nothing was removed, so nothing is drawn for it");
        assertEquals(sum(sweeps), 360.0, 0.0001);
    }

    @Test
    public void theWholeRingGoesToTheOnlyRunItemStatusThereIs() {
        final double[] sweeps = RunItemStatusDonut.sweeps(SLICES, testRunFigures(550, 0, 0, 0));

        assertEquals(sweeps[0], 360.0, 0.0001, "every test case passed");
    }

    @Test
    public void aTestRunWithNothingInItDrawsNoArcsAndDividesByNothing() {
        final double[] sweeps = RunItemStatusDonut.sweeps(SLICES, NodeFigures.NONE);

        assertEquals(sum(sweeps), 0.0, 0.0, "an empty test run leaves the bare track showing");
    }

    @Test
    public void aNodeWithNoSlicesHasNoRing() {
        assertEquals(RunItemStatusDonut.sweeps(DonutSlices.CHILDREN.getSlices(),
                        NodeFigures.ofChildren(9, 4, 2770, 2770, 2)).length, 0,
                "a container's counts are not parts of one whole, so nothing is drawn through them");
    }
}
