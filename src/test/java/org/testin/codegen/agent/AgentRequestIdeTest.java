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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class AgentRequestIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_CLASS = "nafath.LoginTest";

    private void anAgentIsChosen() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String before = settings.agentCommand;
        settings.agentCommand = "no-such-agent";
        Disposer.register(getTestRootDisposable(), () -> settings.agentCommand = before);
    }

    private void aBodyWrittenInto(final @NotNull TestCase testCase) {
        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_CLASS, testCase);
        final @NotNull PsiCodeBlock body = Optional.ofNullable(testMethod.getBody()).orElseThrow(() -> new AssertionError("the generated method has no body"));
        final @NotNull Runnable writeTheBody = () -> body.replace(JavaPsiFacade.getElementFactory(getProject()).createCodeBlockFromText("{ System.out.println(\"signed in\"); }", testMethod));
        WriteCommandAction.runWriteCommandAction(getProject(), writeTheBody);
    }

    private @NotNull String theRequestSentTo(final @NotNull RecordedAgentTab tab) {
        Await.until("no request reached the agent", () -> !tab.getTypedLines().isEmpty());
        assertEquals("the agent was not sent one request", 1, tab.getTypedLines().size());
        return tab.getTypedLines().getFirst();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084
    public void testWritingBodiesSendsOneRequestHoldingEveryTestCaseWhoseMethodIsEmpty() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        anAgentIsChosen();
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCase> both = List.of(createdTestCase(login, "Sign in", "a"), createdTestCase(login, "Sign out", "b"));
        settled();

        WriteBodies.forAll(getProject(), both);

        final @NotNull String request = theRequestSentTo(tab);
        assertTrue("the request does not hold both test cases: " + request, request.contains("Sign in") && request.contains("Sign out"));
        assertFalse("the request asks to write over a body no one wrote", request.contains(Bundle.message("agent.request.write.over")));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-091
    public void testABodyTheTesterWroteIsWrittenOverOnlyOnceTheyConfirm() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        anAgentIsChosen();
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase signOut = createdTestCase(login, "Sign out", "b");
        settled();
        aBodyWrittenInto(signIn);

        WriteBodies.forAll(getProject(), List.of(signIn, signOut));
        ShownDialog.waitedFor(getProject(), ConfirmDialog.class);
        assertTrue("a body was asked for before the tester said to write over", tab.getTypedLines().isEmpty());
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        final @NotNull String request = theRequestSentTo(tab);
        assertTrue("the request does not ask to write over the body the tester wrote: " + request, request.contains(Bundle.message("agent.request.write.over")));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-091
    public void testLeavingABodyTheTesterWroteAsksOnlyForTheEmptyOnes() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        anAgentIsChosen();
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase signOut = createdTestCase(login, "Sign out", "b");
        settled();
        aBodyWrittenInto(signIn);

        WriteBodies.forAll(getProject(), List.of(signIn, signOut));
        ShownDialog.waitedFor(getProject(), ConfirmDialog.class);
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Escape);

        final @NotNull String request = theRequestSentTo(tab);
        assertTrue("the request does not ask for the empty one: " + request, request.contains("Sign out"));
        assertFalse("the request asks for the body the tester left: " + request, request.contains("Sign in"));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086, Rule-CODEGEN-091
    public void testTheRequestHoldsEachTestCaseInOrderWithItsFileAndSaysWhichBodyToWriteOver() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase signOut = createdTestCase(login, "Sign out", "b");

        final @NotNull String request = WriteBodies.requestFor(getProject(), new AgentConnection("claude", "", ""), List.of(signIn), List.of(signOut));

        final @NotNull String file = writtenMethodOf("nafath.LoginPageTest", signIn).getContainingFile().getVirtualFile().getPresentableUrl();
        final @NotNull String writeOver = Bundle.message("agent.request.write.over");
        assertTrue("the test cases are not asked for in order: " + request, request.contains("Test case: Sign in") && request.indexOf("Test case: Sign in") < request.indexOf("Test case: Sign out"));
        assertTrue("the request does not name the file the agent edits: " + request, request.contains(file));
        assertEquals("only the body the tester chose to write over is to be replaced: " + request, request.indexOf(writeOver), request.lastIndexOf(writeOver));
        assertTrue("the test case whose body is the tester's own is not the one told to replace it: " + request, request.indexOf(writeOver) > request.indexOf("Test case: Sign out"));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-089, Rule-CODEGEN-090, Rule-CODEGEN-129
    public void testANewTabRunsTheAgentAndTheWholeRequestIsTypedIntoItAndSaysSent() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull String request = "Write the body of signIn.\nTest case: تسجيل الدخول\nSteps: Open the page";

        AgentRequest.send(getProject(), new AgentConnection("no-such-agent", "--model sonnet", ""), request);

        assertEquals("one tab was not opened for the agent", 1, tab.getOpenedWith().size());
        assertEquals("the tab is not named after the command in settings", List.of("no-such-agent"), tab.getNames());
        final @NotNull List<String> commandLine = tab.getOpenedWith().getFirst();
        assertEquals("the tab does not run exactly the command and arguments in settings", List.of("no-such-agent", "--model", "sonnet"), commandLine);
        assertEquals("the request was not pasted whole and sent once", List.of(AgentRequest.pastedAs(request)), tab.getTypedLines());
        assertTrue("handing the request over did not say Sent: " + said.shown(), said.shown().contains(Done.SENT.getOutcome()));
    }

    // Rule-CODEGEN-129
    public void testARunningTabGetsTheNextRequestTypedIntoIt() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        final @NotNull AgentConnection connection = new AgentConnection("no-such-agent", "", "");
        AgentRequest.send(getProject(), connection, "the first request");

        AgentRequest.send(getProject(), connection, "the second request");

        assertEquals("a second tab was opened beside the running one", 1, tab.getOpenedWith().size());
        assertEquals("the running tab did not get the second request typed into it", List.of(AgentRequest.pastedAs("the first request"), AgentRequest.pastedAs("the second request")), tab.getTypedLines());
    }

    // Rule-CODEGEN-129
    public void testAnotherCommandOpensItsOwnTabNamedAfterIt() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        AgentRequest.send(getProject(), new AgentConnection("claude", "", ""), "the first request");

        AgentRequest.send(getProject(), new AgentConnection(Path.of("bin", "pi.cmd").toString(), "", ""), "the second request");

        assertEquals("a request for another agent went into the first agent's tab", List.of("claude", "pi.cmd"), tab.getNames());
        assertEquals("a request for another agent was not typed into its own new tab", 2, tab.getOpenedWith().size());
    }

    // Rule-CODEGEN-129
    public void testWithoutTheTerminalPluginTheAgentIsNotAskedAndTestinSaysWhy() {
        final @NotNull RecordedAgentTab tab = RecordedAgentTab.installed(getProject(), getTestRootDisposable());
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        OptionalPlugin.TERMINAL.missingUntil(getTestRootDisposable());
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String before = settings.agentCommand;
        settings.agentCommand = "no-such-agent";
        Disposer.register(getTestRootDisposable(), () -> settings.agentCommand = before);

        WriteBodies.forAll(getProject(), List.of(createdTestCase(createdTestSet("Login page"), "Sign in", "a")));

        assertTrue("Testin did not say the Terminal plugin is missing: " + said.shown(), said.shown().stream().anyMatch(balloon -> balloon.contains(Bundle.message("plugin.terminal.requirement"))));
        assertTrue("the agent was asked without the Terminal plugin", tab.getOpenedWith().isEmpty() && tab.getTypedLines().isEmpty());
    }
}
