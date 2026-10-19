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

import java.util.List;

import static org.testng.Assert.assertEquals;

public class TestCaseFieldParserStepsTest {

    // Rule-EDITOR-PANEL-005
    @Test
    public void aStepHoldingANumberAndADashIsOneStep() {
        assertEquals(TestCaseFieldParser.steps("Enter the 2-digit code"), List.of("Enter the 2-digit code"),
                "an untouched cell was rewritten as two steps, and the import stored them");
        assertEquals(TestCaseFieldParser.steps("Wait 5-10 seconds"), List.of("Wait 5-10 seconds"));
    }

    // Rule-EDITOR-PANEL-005
    @Test
    public void aNumberAtTheStartOfAStepIsKept() {
        assertEquals(TestCaseFieldParser.steps("3. Open the menu"), List.of("3. Open the menu"));
    }

    // Rule-EDITOR-PANEL-005
    @Test
    public void eachLineIsOneStep() {
        assertEquals(TestCaseFieldParser.steps("Open the login page\nEnter a valid user\nSubmit"), List.of("Open the login page", "Enter a valid user", "Submit"));
    }
}
