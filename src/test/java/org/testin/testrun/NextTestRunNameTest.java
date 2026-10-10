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

import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;

public class NextTestRunNameTest {

    // Rule-TREE-PANEL-071
    @Test
    public void aTrailingNumberIsIncremented() {
        assertEquals(NextTestRunName.after("cycle-1", Set.of()), "cycle-2");
        assertEquals(NextTestRunName.after("cycle 9", Set.of()), "cycle 10");
        assertEquals(NextTestRunName.after("2", Set.of()), "3");
    }

    @Test
    public void aPaddedNumberIsStillOneNumber() {
        assertEquals(NextTestRunName.after("run09", Set.of()), "run10");
    }

    // Rule-TREE-PANEL-071
    @Test
    public void aNameWithNoNumberGetsOne() {
        assertEquals(NextTestRunName.after("smoke", Set.of()), "smoke-2");
    }

    // Rule-TREE-PANEL-071
    @Test
    public void aTakenNameIsCountedPast() {
        assertEquals(NextTestRunName.after("cycle-1", Set.of("cycle-2", "cycle-3")), "cycle-4");
        assertEquals(NextTestRunName.after("smoke", Set.of("smoke-2")), "smoke-3");
    }

    @Test
    public void aNumberTooLongToParseWholeStillCountsUp() {
        assertEquals(NextTestRunName.after("build-12345678901234567890", Set.of()), "build-12345678901234567891");
    }
}
