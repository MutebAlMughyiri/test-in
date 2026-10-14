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

package org.testin.codegen;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.actions.TestinDataKeys;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public class AutomateTestCaseIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String CHECKOUT_LOGIN_TEST = "nafath.checkout.LoginTest";

    private @NotNull AnAction automate() {
        return Optional.ofNullable(ActionManager.getInstance().getAction("Testin.AutomateTestCase")).orElseThrow(() -> new AssertionError("Automate Test Case is not registered"));
    }

    private @NotNull AnActionEvent selecting(final @NotNull TestCase testCase) {
        return TestActionEvent.createTestEvent(automate(), SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, List.of(testCase))
                .build());
    }

    private @NotNull AnActionEvent updated(final @NotNull TestCase testCase) {
        final @NotNull AnActionEvent e = selecting(testCase);
        ActionUtil.updateAction(automate(), e);
        return e;
    }

    // Rule-CODEGEN-071
    public void testTheEntryIsGrayWithTheReasonWhereThereIsNoMethodToWrite() {
        final @NotNull TestSetNode login = indexedTestSet("Login", theTestCasesDirectory());

        final @NotNull AnActionEvent noDescription = updated(indexedTestCase(login, "", "b"));
        assertTrue("the entry was left off the menu", noDescription.getPresentation().isVisible());
        assertFalse("the entry is live for a test case it cannot write a method for", noDescription.getPresentation().isEnabled());
        assertEquals("the gray entry does not say why", Bundle.message("automate.no.description.description"), noDescription.getPresentation().getDescription());

        assertTrue("the entry is gray for a test case it can write a method for", updated(indexedTestCase(login, "Log in", "c")).getPresentation().isEnabled());
    }

    // Rule-CODEGEN-025
    public void testAutomateWritesTheMethodAndItsClassAndTellsATestCaseThatHasOne() {
        final @NotNull TestCase testCase = indexedTestCase(indexedTestSet("Login", indexedPackage("Checkout", theTestCasesDirectory())), "Log in with a valid user", "b");

        ActionUtil.performAction(automate(), selecting(testCase));
        Await.until("Automate Test Case wrote no method, class or package folder", () -> methodOf(CHECKOUT_LOGIN_TEST, testCase).isPresent());

        final @NotNull AutomationState state = Services.getInstance(getProject(), AutomationState.class);
        state.read(getProject(), List.of(testCase), () -> {
        });
        Await.until("the automation state never learned the test case has a method", () -> state.hasMethod(testCase.getUuid()));

        final @NotNull AnActionEvent again = updated(testCase);
        assertFalse("Automate Test Case is live for a test case whose method is already written", again.getPresentation().isEnabled());
        assertEquals("the tester is not told the method is already there", Bundle.message("automate.already.written.description"), again.getPresentation().getDescription());
    }
}
