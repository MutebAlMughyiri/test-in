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

import org.testng.annotations.Test;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RankTest {

    @Test
    public void theFirstTestCaseInAnEmptySetGetsRoomOnBothSides() {
        final String first = Rank.between("", "");

        assertTrue(first.compareTo(Rank.between("", first)) > 0, "something fits before it");
        assertTrue(first.compareTo(Rank.after(first)) < 0, "something fits after it");
    }

    @Test
    public void appendingSortsAfterWhatIsThere() {
        String last = Rank.between("", "");

        for (int i = 0; i < 200; i++) {
            final String next = Rank.after(last);
            assertTrue(last.compareTo(next) < 0, "append " + i + ": " + last + " then " + next);
            last = next;
        }
    }

    // Rule-INTERNAL-032
    @Test
    public void aRankAlwaysFitsBetweenTwoOthers() {
        String low = Rank.between("", "");
        String high = Rank.after(low);

        for (int i = 0; i < 200; i++) {
            final String middle = Rank.between(low, high);

            assertTrue(low.compareTo(middle) < 0, "drop " + i + ": " + low + " < " + middle);
            assertTrue(middle.compareTo(high) < 0, "drop " + i + ": " + middle + " < " + high);

            high = middle;
        }
    }

    @Test
    public void aRankFitsBeforeTheFirstTestCase() {
        String first = Rank.between("", "");

        for (int i = 0; i < 100; i++) {
            final String earlier = Rank.between("", first);

            assertTrue(earlier.compareTo(first) < 0, "insert at top " + i + ": " + earlier + " < " + first);
            first = earlier;
        }
    }

    @Test
    public void appendingPastTheAlphabetGrowsTheRank() {
        assertEquals(Rank.after("y"), "z");
        assertEquals(Rank.after("z"), "zm");
        assertEquals(Rank.after("zz"), "zzm");

        assertTrue("z".compareTo(Rank.after("z")) < 0, "and the longer rank still sorts after");
    }
}
