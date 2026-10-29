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

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.testin.navigate.TestCaseIdentity;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RobotSuiteTest {

    private static final @NotNull String SUITE = """
            *** Settings ***
            Test Tags    0c7d6b1a-9c1b-4e2a-9f31-3f2a05c18b44
            
            *** Test Cases ***
            Pay with a saved card
                [Tags]    3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b    smoke
                # TODO: Auto-generated test steps for Pay with a saved card
                No Operation
            
            # written by hand
            Pay with a new card
            \t[Tags]\t9b1d3c5e-2a4f-4b6d-8e0f-1a2b3c4d5e6f
                Open Checkout    ${CART}
            
            Pay by bank transfer
                [Tags]    payments    0c7d6b1a-9c1b-4e2a-9f31-3f2a05c18b44
            """;

    private static int tagsOf(final @NotNull String testName) {
        return SUITE.indexOf("[Tags]", SUITE.indexOf(testName));
    }

    private static @NotNull List<UUID> testCasesIn(final @NotNull String text, final @NotNull TextRange range) {
        return RobotSuite.testCasesIn(text, range).stream().map(TestCaseIdentity::testCaseUuid).toList();
    }

    // Rule-CODEGEN-029, Rule-CODEGEN-118
    @Test
    public void aTestIsATestCaseByTheUuidThatIsItsFirstTag() {
        assertEquals(testCasesIn(SUITE, new TextRange(0, SUITE.length())), List.of(UUID.fromString("3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b"), UUID.fromString("9b1d3c5e-2a4f-4b6d-8e0f-1a2b3c4d5e6f")),
                "a tab separates cells as two spaces do; a UUID that is not the first tag, or tags every test carries, name no test case");
        final @NotNull String lowerCase = "Sign in\n    [tags]    3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b\n";
        assertEquals(testCasesIn(lowerCase, new TextRange(0, lowerCase.length())), List.of(UUID.fromString("3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b")), "Robot reads a setting in any capitals");
    }

    // Rule-CODEGEN-028, Rule-CODEGEN-118
    @Test
    public void theMarkSitsOnTheTagsCellOfTheElementThatHoldsIt() {
        final int lineStart = SUITE.lastIndexOf('\n', tagsOf("Pay with a saved card")) + 1;
        final @NotNull List<TestCaseIdentity> token = RobotSuite.testCasesIn(SUITE, new TextRange(lineStart, tagsOf("Pay with a saved card") + 6));

        assertEquals(token.size(), 1, "RobotCode's token for [Tags] starts with the line's indentation and holds one mark");
        assertEquals(token.getFirst().range(), new TextRange(tagsOf("Pay with a saved card"), tagsOf("Pay with a saved card") + 6), "the mark covers the [Tags] cell");
        assertEquals(testCasesIn(SUITE, new TextRange(tagsOf("Pay with a saved card") + 10, tagsOf("Pay with a saved card") + 46)), List.of(), "the UUID's own token holds no second mark");
    }

    // Rule-CODEGEN-118
    @Test
    public void onlyASuiteFileThatMentionsTagsIsRead() {
        assertTrue(RobotSuite.mayHoldTestCases("Payment.robot", SUITE));
        assertTrue(RobotSuite.mayHoldTestCases("Payment.ROBOT", "    [tags]"), "Robot reads a setting in any capitals");
        assertFalse(RobotSuite.mayHoldTestCases("notes.txt", SUITE), "a file that is not a suite holds no Robot test");
        assertFalse(RobotSuite.mayHoldTestCases("Payment.robot", "No Operation"), "an element without [Tags] holds no mark");
    }

    // Rule-CODEGEN-119
    @Test
    public void aTestStartsAtItsNameLine() {
        assertEquals(RobotSuite.testStartAbove(SUITE, tagsOf("Pay with a new card")), Optional.of(SUITE.indexOf("Pay with a new card")), "a comment line above the test is not where it starts");
    }

    // Rule-CODEGEN-119, Rule-CODEGEN-127
    @Test
    public void aCommentLineBetweenTwoTestsIsNoTestOfItsOwn() {
        assertEquals(RobotSuite.blocksOf(SUITE).stream().map(RobotTestBlock::name).toList(), List.of("Pay with a saved card", "Pay with a new card", "Pay by bank transfer"), "a comment line was read as a test");
    }

    // Rule-CODEGEN-003
    @Test
    public void aBodyIsTheTestersOnceItIsAnythingButTheTwoLinesTestinWrote() {
        final @NotNull List<TestCaseIdentity> testCases = RobotSuite.testCasesIn(SUITE, new TextRange(0, SUITE.length()));

        assertFalse(RobotSuite.holdsAWrittenBody(SUITE, testCases.getFirst()), "the TODO and No Operation are what Testin wrote, not the tester");
        assertTrue(RobotSuite.holdsAWrittenBody(SUITE, testCases.getLast()), "a keyword the tester wrote is a written body");
    }

    // Rule-CODEGEN-003
    @Test
    public void twoLinesAreTestinsOnlyWhenTheyAreTheTodoAndNoOperation() {
        final @NotNull String suite = """
                *** Test Cases ***
                Log in
                    [Tags]    1e2d3c4b-5a69-4788-9a0b-1c2d3e4f5a6b
                    [Documentation]    Opens the app
                    # TODO: Auto-generated test steps for Log in
                    No Operation
                Log out
                    [Tags]    2e2d3c4b-5a69-4788-9a0b-1c2d3e4f5a6b
                    # TODO: Auto-generated test steps for Log out
                    Log    done
                Sign up
                    [Tags]    3e2d3c4b-5a69-4788-9a0b-1c2d3e4f5a6b
                    Open    app
                    No Operation
                """;
        final @NotNull List<TestCaseIdentity> testCases = RobotSuite.testCasesIn(suite, new TextRange(0, suite.length()));

        assertFalse(RobotSuite.holdsAWrittenBody(suite, testCases.getFirst()), "a setting under the tags made Testin's two lines the tester's");
        assertTrue(RobotSuite.holdsAWrittenBody(suite, testCases.get(1)), "a TODO followed by a keyword was taken for what Testin wrote");
        assertTrue(RobotSuite.holdsAWrittenBody(suite, testCases.getLast()), "a keyword followed by No Operation was taken for what Testin wrote");
    }

    @Test
    public void aLineWithNoLineBreakEndsWhereTheTextEnds() {
        assertEquals(RobotSuite.lineEnd("Log in", 0), "Log in".length());
    }

    // Rule-CODEGEN-069
    @Test
    public void theTestAboveTheTagsIsNamedByItsFirstCell() {
        assertEquals(RobotSuite.testNameAbove(SUITE, tagsOf("Pay with a saved card")), Optional.of("Pay with a saved card"));
        assertEquals(RobotSuite.testNameAbove(SUITE, tagsOf("Pay with a new card")), Optional.of("Pay with a new card"), "a comment line above the test is not its name");
        assertEquals(RobotSuite.testNameAbove("*** Test Cases ***\n    [Tags]    x\n", 19), Optional.empty(), "tags straight under the section header belong to no test");
    }
}
