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

package org.testin.explorer;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PanelStateTest {

    private static @NotNull PanelState of(final boolean rootConfigured, final boolean indexed, final boolean testProjectResolved, final boolean boundTestProjectMissing, final boolean cloneUrlKnown, final boolean anyTestProjectsUnderRoot) {
        return PanelState.of(rootConfigured, indexed, testProjectResolved, boundTestProjectMissing, cloneUrlKnown, anyTestProjectsUnderRoot);
    }

    // Rule-TREE-PANEL-118
    @Test
    public void nothingIsDecidedBeforeTheIndexIsBuilt() {
        assertEquals(of(true, false, false, false, false, true), PanelState.READING,
                "a name that has not resolved because nothing is indexed yet is not a name that resolves to nothing");

        assertEquals(of(true, false, false, true, true, true), PanelState.READING,
                "the clone offer waits one draw too, rather than being made about a listing the index has not seen");

        assertEquals(of(false, false, false, false, false, false), PanelState.NO_ROOT,
                "with no root nothing indexes at all, so there is nothing to wait for - only a folder to set");
    }

    @Test
    public void aResolvedTestProjectBeatsTheWait() {
        assertEquals(of(true, false, true, false, false, true), PanelState.TREE);
    }

    @Test
    public void noRootBeatsEverything() {
        assertEquals(of(false, false, true, true, true, true), PanelState.NO_ROOT);
        assertEquals(of(false, false, false, false, false, false), PanelState.NO_ROOT);
    }

    @Test
    public void aResolvedTestProjectIsTheTree() {
        assertEquals(of(true, true, true, false, false, true), PanelState.TREE);
        assertEquals(of(true, true, true, false, true, true), PanelState.TREE);
    }

    @Test
    public void aMissingTestProjectWithAUrlIsCloned() {
        assertEquals(of(true, true, false, true, true, false), PanelState.CLONE_BOUND);

        assertEquals(of(true, true, false, true, true, true), PanelState.CLONE_BOUND);
    }

    @Test
    public void aMissingTestProjectWithoutAUrlFallsBack() {
        assertEquals(of(true, true, false, true, false, false), PanelState.NO_TEST_PROJECTS);
        assertEquals(of(true, true, false, true, false, true), PanelState.CHOOSE);
    }

    @Test
    public void anUnboundRepositoryChooses() {
        assertEquals(of(true, true, false, false, false, true), PanelState.CHOOSE);
    }

    @Test
    public void anArchivedTestProjectChooses() {
        assertEquals(of(true, true, false, false, true, true), PanelState.CHOOSE);
    }

    @Test
    public void anEmptyRootCreates() {
        assertEquals(of(true, true, false, false, false, false), PanelState.NO_TEST_PROJECTS);
    }
}
