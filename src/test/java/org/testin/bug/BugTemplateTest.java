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

package org.testin.bug;

import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BugTemplateTest {

    private static final UUID ID = UUID.fromString("07f7e754-b849-4b38-9e6e-a2cacd84e927");
    private static final String LINK = "https://github.com/mtb550/test-03/blob/master/Test%20Cases/ts2/07f7e754-b849-4b38-9e6e-a2cacd84e927.tc";
    private static final byte[] SCREENSHOT = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

    private static @NotNull BugFacts facts() {
        return BugFacts.builder()
                .title("Activate app stores the app version")
                .severity(BugSeverity.MINOR)
                .priority(BugPriority.LOW)
                .platform("Mobile · Backend")
                .actualResult("backend does not store correct App_Version value in db after activate app.")
                .expectedResult("Backend should store the correct value as received from frontend.")
                .steps(List.of("Activate the app from the frontend with version 11.3", "Read App_Version from the database"))
                .testData("App_Version = 11.3")
                .stacktrace("""
                        java.lang.AssertionError: expected [11.3] but found [11.300000190734863]
                            at testProject.ActivateAppTest.version(ActivateAppTest.java:42)
                        """)
                .screenshots(List.of(SCREENSHOT))
                .testRun("Sprint 7 Cycle 3")
                .executed("Muteb · Sunday 13-09-2026 At 14:14:00 [Asia/Riyadh]")
                .browser("")
                .device("Samsung")
                .language("English")
                .commit("")
                .testCaseId(ID)
                .testSetName("ActivateApp")
                .build();
    }

    private static @NotNull BugFacts nothingKnown() {
        return facts().toBuilder()
                .platform("")
                .actualResult("")
                .expectedResult(" ")
                .steps(List.of(""))
                .testData("")
                .stacktrace("")
                .screenshots(List.of())
                .executed("")
                .device("")
                .language("")
                .build();
    }

    // Rule-VIEW-PANEL-068
    @Test
    public void theIssueReadsAsTheDesignSays() {
        final String expected = """
                | Severity     | Priority     | Platform     | Environment      | Build            |
                |:-------------|:-------------|:-------------|:-----------------|:-----------------|
                | 🟡 Minor | ⚪ Low | Mobile · Backend | n\\a | n\\a |
                
                ### Actual result
                
                backend does not store correct App_Version value in db after activate app.
                
                ### Expected result
                
                Backend should store the correct value as received from frontend.
                
                ### Steps to reproduce
                
                1. Activate the app from the frontend with version 11.3
                2. Read App_Version from the database
                
                ### Test data
                
                ```
                App_Version = 11.3
                ```
                
                ### Impact
                
                n\\a
                
                ### Exception
                
                <details>
                <summary>java.lang.AssertionError: expected [11.3] but found [11.300000190734863]</summary>
                
                ```
                java.lang.AssertionError: expected [11.3] but found [11.300000190734863]
                    at testProject.ActivateAppTest.version(ActivateAppTest.java:42)
                ```
                </details>
                
                ### Screenshots
                
                ![Screenshot 1](./screenshot-1.png)
                
                ### Where it was found
                
                |                                 |                                      |
                |:--------------------------------|:-------------------------------------|
                | **Test run**                    | Sprint 7 Cycle 3                          |
                | **Executed**                    | Muteb · Sunday 13-09-2026 At 14:14:00 [Asia/Riyadh]                         |
                | **Browser · Device · Language** | n\\a · Samsung · English            |
                | **Commit**                      | n\\a                           |
                | **Test case**                   | [07f7e754](LINK) in test set ActivateApp |
                
                ---
                <sub>#ActivateApp · Reported with Testin</sub>
                """.replace("LINK", LINK);

        assertEquals(BugTemplate.body(facts(), Optional.of(LINK)), expected);
    }

    // Rule-VIEW-PANEL-068
    @Test
    public void everyPartTestinCannotGetSaysNotAvailable() {
        final String body = BugTemplate.body(nothingKnown(), Optional.empty());

        assertEquals(body.split("n\\\\a", -1).length - 1, 15,
                "3 in the summary, 7 sections and 5 in where it was found - 3 of them always");
        assertTrue(body.contains("| **Test case**                   | 07f7e754 in test set ActivateApp |"), "the test case shows without its link");
    }

    @Test
    public void everyPlaceholderIsFilled() {
        assertFalse(BugTemplate.body(nothingKnown(), Optional.empty()).contains("{{"));
    }

    @Test
    public void aPlaceholderATesterTypedIsWrittenAsText() {
        final String body = BugTemplate.body(facts().toBuilder().actualResult("{{severity}}").build(), Optional.empty());

        assertTrue(body.contains("""
                ### Actual result
                
                {{severity}}
                """));
    }

    @Test
    public void aCellKeepsItsTableWhole() {
        assertEquals(BugTemplate.cell("a|b\\|c\nd\r\ne"), "a\\|b\\\\\\|c<br>d<br>e");
    }

    @Test
    public void sectionTextCannotBecomeMarkup() {
        assertEquals(BugTemplate.section("""
                        # not a heading
                        text
                        ---
                          ===
                        * * *
                        @someone fixed #12 in <script> for me@example.com"""),
                """
                        \\# not a heading
                        text
                        \\---
                          \\===
                        \\* * *
                        @&#8203;someone fixed #&#8203;12 in &lt;script> for me@example.com""");
    }

    @Test
    public void aCellMentionsNobody() {
        assertEquals(BugTemplate.cell("@team/qa and #7"), "@&#8203;team/qa and #&#8203;7");
    }

    @Test
    public void aFenceIsLongerThanAnyBackticksInside() {
        assertEquals(BugTemplate.codeBlock("""
                a ``` b
                `````
                
                """), """
                ``````
                a ``` b
                `````
                ``````""");
        assertEquals(BugTemplate.codeBlock("user=`x`"), """
                ```
                user=`x`
                ```""");
    }

    @Test
    public void theSummaryIsTheFirstLineThatSaysAnythingEscaped() {
        final String exception = BugTemplate.exception("""
                
                
                Error <init> & "x" @bob #12
                  at y""");

        assertTrue(exception.startsWith("""
                <details>
                <summary>Error &lt;init&gt; &amp; &quot;x&quot; @&#8203;bob #&#8203;12</summary>
                
                ```
                """), exception);
    }

    @Test
    public void stepsAreNumberedInTurnAndALongStepStaysInsideIts() {
        assertEquals(BugTemplate.steps(List.of("Open", "", "Type\nuser", "# 3")), """
                1. Open
                2. Type
                   user
                3. \\# 3""");
    }

    // Rule-VIEW-PANEL-076
    @Test
    public void everyScreenshotIsReferencedByTheFileItIsAttachedAs() {
        final BugFacts two = facts().toBuilder().screenshots(List.of(SCREENSHOT, SCREENSHOT)).build();

        assertTrue(BugTemplate.body(two, Optional.empty()).contains("""
                ### Screenshots
                
                ![Screenshot 1](./screenshot-1.png)
                ![Screenshot 2](./screenshot-2.png)
                """));
        assertEquals(BugTemplate.screenshotFile(2), "screenshot-2.png");
    }

    @Test
    public void theTagIsTheTestSetNameWithoutSpaces() {
        final String body = BugTemplate.body(facts().toBuilder().testSetName("Activate App").build(), Optional.empty());

        assertTrue(body.endsWith("<sub>#ActivateApp · Reported with Testin</sub>\n"));
        assertTrue(body.contains("in test set Activate App |"));
    }
}
