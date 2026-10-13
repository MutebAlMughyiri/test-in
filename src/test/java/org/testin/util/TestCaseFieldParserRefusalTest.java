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

package org.testin.util;

import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCaseFieldParserRefusalTest {

    // Rule-SHARE-106
    @Test
    public void aPriorityItCannotReadIsRefused() {
        assertTrue(TestCaseFieldParser.priority("Urgent", Priority.HIGH).isEmpty(),
                "answering Low is how 200 imported test cases all became the lowest priority");
    }

    // Rule-SHARE-106
    @Test
    public void aBlankPriorityKeepsWhatTheTestCaseHad() {
        assertEquals(TestCaseFieldParser.priority("  ", Priority.HIGH).orElseThrow(), Priority.HIGH,
                "a priority has no empty form, so a blank cell says nothing rather than saying Low");
    }

    @Test
    public void aPriorityIsReadByItsWordInAnyCapitals() {
        assertEquals(TestCaseFieldParser.priority(Priority.HIGH.getLabel(), Priority.LOW).orElseThrow(), Priority.HIGH,
                "the word is what the column shows, so it is what a tester retypes");
        assertEquals(TestCaseFieldParser.priority("high", Priority.LOW).orElseThrow(), Priority.HIGH,
                "a tester typing the word in lower case is still understood");
    }

    @Test
    public void aGroupNobodyHasUsedBeforeIsTaken() {
        assertEquals(TestCaseFieldParser.groups("Regression, Payments").orElseThrow(), List.of("Regression", "Payments"));
    }

    @Test
    public void aGroupNamedTwiceIsNamedOnce() {
        assertEquals(TestCaseFieldParser.groups("Regression, Regression").orElseThrow(), List.of("Regression"),
                "the bulk editor deduplicated and the grid did not, so one cell had two answers (#295)");
    }

    // Rule-SHARE-106
    @Test
    public void aBlankCellIsNoGroupsAtAll() {
        assertTrue(TestCaseFieldParser.groups("   ").orElseThrow().isEmpty(),
                "groups have an empty form, so a blank cell clears them");
    }

    @Test
    public void theNoGroupWordIsNoGroupsEither() {
        assertTrue(TestCaseFieldParser.groups(Groups.NONE).orElseThrow().isEmpty());
    }
}
