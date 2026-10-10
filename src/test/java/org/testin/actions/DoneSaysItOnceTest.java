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

package org.testin.actions;

import org.testin.notifications.Done;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class DoneSaysItOnceTest {

    // Rule-TREE-PANEL-007
    @Test
    public void aChangeIsSaidInThePastTenseAndSeveralWithACount() {
        assertEquals(Done.CREATED.getOutcome(), "Created");
        assertEquals(Done.RENAMED.getOutcome(), "Renamed");
        assertEquals(Done.REMOVED.getOutcome(), "Removed");

        assertEquals(Done.counted(Done.REMOVED.getOutcome(), 4), "Removed 4", "four removals are not one message with a count");
        assertEquals(Done.counted(Done.REMOVED.getOutcome(), 1), "Removed", "one removal is counted");
    }
}
