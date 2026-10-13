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

package org.testin.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PriorityDefaultTest {

    // Rule-EDITOR-PANEL-031
    @Test
    public void aNewTestCaseStartsAtTheDefaultPriorityWhichIsTheLowestChoice() {
        assertEquals(Priority.DEFAULT, Priority.CHOICES.getLast(), "the default priority is not the lowest the tester can choose");
        assertEquals(new TestCase().getPriority(), Priority.DEFAULT, "a new test case does not start at the default priority");
        assertEquals(TestCase.builder().build().getPriority(), Priority.DEFAULT, "a built test case does not start at the default priority");
    }
}
