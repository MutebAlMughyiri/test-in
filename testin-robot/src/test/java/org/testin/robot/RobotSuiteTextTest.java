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

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RobotSuiteTextTest {

    private static final @NotNull UUID SAVED = UUID.fromString("3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b");

    private static final @NotNull UUID NEW = UUID.fromString("9b1d3c5e-2a4f-4b6d-8e0f-1a2b3c4d5e6f");

    private static final @NotNull UUID TRANSFER = UUID.fromString("0c7d6b1a-9c1b-4e2a-9f31-3f2a05c18b44");

    private static final @NotNull String SUITE = """
            *** Settings ***
            Library    SeleniumLibrary
            
            *** Test Cases ***
            Pay with a saved card
                [Tags]    3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b    smoke
                Open Checkout
            
            Written by hand
                Log    not Testin's
            
            Pay with a new card
                [Tags]    9b1d3c5e-2a4f-4b6d-8e0f-1a2b3c4d5e6f
                # TODO: Auto-generated test steps for Pay with a new card
                No Operation
            
            *** Keywords ***
            Open Checkout
                No Operation
            """;

    private static @NotNull RobotTestBlock test(final @NotNull String text, final @NotNull UUID testCaseUuid) {
        return RobotSuite.blockOf(text, testCaseUuid).orElseThrow(() -> new AssertionError("no test for " + testCaseUuid));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-124
    @Test
    public void aNewTestHoldsItsTagsAndTheTwoLinesTestinWrites() {
        assertEquals(RobotSuiteText.withTest("", RobotSuiteText.newTest("Pay by bank transfer", List.of(TRANSFER.toString(), "payments", "robot:exclude"))), """
                *** Test Cases ***
                Pay by bank transfer
                    [Tags]    0c7d6b1a-9c1b-4e2a-9f31-3f2a05c18b44    payments    robot:exclude
                    # TODO: Auto-generated test steps for Pay by bank transfer
                    No Operation
                """);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-126, Rule-CODEGEN-127
    @Test
    public void aTestIsAddedToTheTestCasesSectionAndEveryOtherLineStays() {
        final @NotNull String added = RobotSuiteText.withTest(SUITE, RobotSuiteText.newTest("Pay by bank transfer", List.of(TRANSFER.toString())));

        assertTrue(added.contains("    No Operation\n\nPay by bank transfer\n"), "the new test is not after the last test, a blank line apart: " + added);
        assertTrue(added.contains("    No Operation\n\n*** Keywords ***\n"), "the new test is not before the Keywords section: " + added);
        assertEquals(added.replace(RobotSuiteText.newTest("Pay by bank transfer", List.of(TRANSFER.toString())) + "\n", ""), SUITE, "a line Testin does not own changed");
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067, Rule-CODEGEN-126
    @Test
    public void ordering_movesOnlyTestinsTestsIntoTheSetsOrder() {
        final @NotNull String arranged = RobotSuiteText.arranged(SUITE, List.of(NEW, SAVED));

        assertTrue(arranged.indexOf("Pay with a new card") < arranged.indexOf("Pay with a saved card"), "the tests are not in the set's order: " + arranged);
        assertTrue(arranged.indexOf("Written by hand") < arranged.indexOf("Pay with a saved card") && arranged.indexOf("Written by hand") > arranged.indexOf("Pay with a new card"), "a test the tester wrote moved: " + arranged);
        assertEquals(RobotSuiteText.arranged(SUITE, List.of(SAVED, NEW)), SUITE, "a suite already in order changed");
    }

    // UC-CODEGEN-010, UC-CODEGEN-012, UC-CODEGEN-014, Rule-CODEGEN-122, Rule-CODEGEN-123, Rule-CODEGEN-127
    @Test
    public void renamingRetaggingAndRemovingTouchOnlyTheLinesTestinOwns() {
        assertTrue(RobotSuiteText.renamed(SUITE, test(SUITE, SAVED), "Pay with a stored card").contains("\nPay with a stored card\n    [Tags]    3f2a05c1"));
        assertTrue(RobotSuiteText.retagged(SUITE, test(SUITE, NEW), List.of(NEW.toString(), "regression")).contains("    [Tags]    9b1d3c5e-2a4f-4b6d-8e0f-1a2b3c4d5e6f    regression\n    # TODO"));

        final @NotNull String withoutTags = "*** Test Cases ***\nHand made\n    Log    x\n";
        final @NotNull RobotTestBlock handMade = RobotSuite.blocksOf(withoutTags).getFirst();
        assertEquals(RobotSuiteText.retagged(withoutTags, handMade, List.of(SAVED.toString())), "*** Test Cases ***\nHand made\n    [Tags]    3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b\n    Log    x\n", "a test with no [Tags] line did not get one under its name");

        final @NotNull String removed = RobotSuiteText.withoutTest(SUITE, test(SUITE, SAVED));
        assertTrue(!removed.contains("Pay with a saved card") && removed.contains("Written by hand") && removed.contains("Open Checkout\n    No Operation"), "removing a test removed more than it: " + removed);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-078
    @Test
    public void aCopyCarriesTheOriginalsStepsUnderItsOwnNameAndTags() {
        assertEquals(RobotSuiteText.copyOf(RobotSuiteText.testText(SUITE, test(SUITE, SAVED)), "Pay with a saved card (copy)", List.of(TRANSFER.toString())), """
                Pay with a saved card (copy)
                    [Tags]    0c7d6b1a-9c1b-4e2a-9f31-3f2a05c18b44
                    Open Checkout
                """);
    }
}
