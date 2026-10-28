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

package org.testin.codegen.agent;


import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.model.TestCase;
import org.testin.model.node.NodeChain;
import org.testin.model.node.TestSetNode;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AgentCommandTest {

    private static @NotNull TestCase aTestCase() {
        return TestCase.builder()
                .uuid(UUID.randomUUID())
                .description("Sign in with a valid account")
                .expectedResult("The dashboard opens")
                .testData("1098765432 / Passw0rd!")
                .preConditions("The account is active")
                .module("Accounts")
                .build();
    }

    private static @NotNull AgentConnection connection(final String command) {
        return new AgentConnection(command, "-p", "");
    }

    // Rule-CODEGEN-083
    @Test
    public void theArgumentsAreTakenAsTyped() {
        assertEquals(AgentCli.arguments("  --model   sonnet "), List.of("--model", "sonnet"));
    }

    // Rule-CODEGEN-083
    @Test
    public void anAgentWithNoArgumentsIsStartedWithNoneOfThem() {
        assertEquals(AgentCli.arguments(""), List.of());
    }

    // Rule-CODEGEN-083
    @Test
    public void anEmptyCommandIsNoAgent() {
        assertFalse(connection("").isConnected(), "an empty command is how a tester says there is no agent");
        assertTrue(connection("pi").isConnected());
    }

    // Rule-CODEGEN-086
    @Test
    public void thePromptCarriesTheTestCaseAndNothingElse() {
        final TestCase testCase = aTestCase();
        final String written = BodyPrompt.of("{method}: {description} / {expectedResult} / {testData} / {preConditions} / {module} / {steps}", testCase, "signInWithAValidAccount", "");

        assertEquals(written, "signInWithAValidAccount: Sign in with a valid account / The dashboard opens"
                + " / 1098765432 / Passw0rd! / The account is active / Accounts / ");
        assertFalse(written.contains(testCase.getUuid().toString()), "the test case UUID is not the agent's business");
    }

    // Rule-CODEGEN-086
    @Test
    public void thePromptSaysWhereTheTestCaseSitsAndWhichClassHoldsItsMethod() {
        final TestSetNode login = NodeChain.testSetAt("NAFATH", "Test Cases", "Accounts", "Login");
        final TestCase testCase = aTestCase();
        testCase.setParent(login);

        assertEquals(BodyPrompt.of("{testSetPath} | {class}", testCase, "signInWithAValidAccount", ""), "NAFATH > Test Cases > Accounts > Login | " + Fqcn.classOfMethod(testCase));
        assertFalse(Fqcn.classOfMethod(testCase).isEmpty(), "a class that does not exist yet is still named");
    }

    // Rule-CODEGEN-086
    @Test
    public void thePromptNamesTheFileTheMethodIsIn() {
        assertEquals(BodyPrompt.of("Edit {method} in {file}", aTestCase(), "signInWithAValidAccount", "LoginPageTest.java"), "Edit signInWithAValidAccount in LoginPageTest.java");
    }

    // Rule-CODEGEN-084
    @Test
    public void aNewTabRunsExactlyTheCommandAndArgumentsInSettings() {
        assertEquals(AgentRequest.commandLine(new AgentConnection("no-such-agent", "--model sonnet", "")), List.of("no-such-agent", "--model", "sonnet"));
    }

    // Rule-CODEGEN-084
    @Test
    public void theRequestIsPastedAsTheTerminalPastesAndSentOnce() {
        final String escape = Character.toString(0x1b);
        assertEquals(AgentRequest.pastedAs("Test case: Sign in\r\nSteps: Open the page\n"), escape + "[200~Test case: Sign in\rSteps: Open the page" + escape + "[201~\r",
                "the request goes as the IDE's Terminal sends a Ctrl+V paste, so every agent keeps its lines together, and one Enter sends it");
    }

    // Rule-CODEGEN-086
    @Test
    public void aTemplateNobodyEditedIsTheOneTestinShips() {
        final AgentConnection typed = new AgentConnection("pi", "-p", "   ");

        assertTrue(typed.promptTemplate().contains("{description}"),
                "a blank prompt means the shipped one, so a later release's better wording reaches a tester who never edited it");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    @Test
    public void aTemplateTheTesterEditedIsTheirs() {
        assertEquals(new AgentConnection("pi", "-p", "Write {description} with Selenide").promptTemplate(), "Write {description} with Selenide");
    }
}
