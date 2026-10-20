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

package org.testin.robot;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class RobotNamesTest {

    // Rule-CODEGEN-121
    @Test
    public void aFolderOrSuiteFileShowsTheNameTheTreeShows() {
        assertEquals(RobotNames.fileNameOf("Payment flow"), "Payment_flow", "a space becomes an underscore, and case is kept");
        assertEquals(RobotNames.fileNameOf("NAFATH-test-cases"), "NAFATH", "a trailing -test-cases is dropped");
        assertEquals(RobotNames.fileNameOf("Pay: card?"), "Pay_card", "a character a file name cannot hold is left out");
        assertEquals(RobotNames.fileOf(List.of("Demo", "Test Cases", "Checkout", "Payment flow")), Optional.of("Demo/Checkout/Payment_flow.robot"), "the Test Cases folder is not a folder of the code");
        assertEquals(RobotNames.folderOf(List.of("Demo", "Test Cases", "Checkout")), Optional.of("Demo/Checkout"));
        assertEquals(RobotNames.fileOf(List.of("Demo", "Test Cases", "???")), Optional.empty(), "a name that comes to nothing has no file");
    }

    // Rule-CODEGEN-122
    @Test
    public void aTestIsNamedByItsDescriptionInAFormRobotReadsAsOneName() {
        assertEquals(RobotNames.testNameOf("  Pay   with\ta  card "), Optional.of("Pay with a card"), "two spaces would end a cell, so a run of spaces is one space");
        assertEquals(RobotNames.testNameOf("# Pay with a card"), Optional.of("Pay with a card"), "Robot reads a line starting with # as a comment");
        assertEquals(RobotNames.testNameOf("[Setup] first"), Optional.of("Setup] first"), "Robot reads a line starting with [ as a setting");
        assertEquals(RobotNames.testNameOf("..."), Optional.empty(), "a description that comes to nothing has no test");
    }
}
