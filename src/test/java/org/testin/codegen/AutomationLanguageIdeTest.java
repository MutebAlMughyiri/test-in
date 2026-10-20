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
import org.testin.actions.TestinDataKeys;
import org.testin.config.AutomationLanguage;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Optional;

public class AutomationLanguageIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String NOT_YET = "(not available for Robot Framework yet)";

    private @NotNull AnActionEvent theMenuEntry(final @NotNull String actionId, final @NotNull TestCase testCase) {
        final @NotNull AnAction action = Optional.ofNullable(ActionManager.getInstance().getAction(actionId)).orElseThrow(() -> new AssertionError(actionId + " is not registered"));
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, List.of(testCase))
                .build());
        ActionUtil.updateAction(action, e);
        return e;
    }

    // Rule-CODEGEN-116
    public void testATestProjectIsRobotFrameworkWhenItsInterpreterIsPythonAndJavaOtherwise() {
        assertEquals("the test project of a Java code project is not Java", AutomationLanguage.JAVA, AutomationLanguage.of(getProject()));

        interpreterIs("Python SDK");
        assertEquals("a Python interpreter did not make the test project Robot Framework", AutomationLanguage.ROBOT_FRAMEWORK, AutomationLanguage.of(getProject()));

        interpreterIs("COBOL SDK");
        assertEquals("an interpreter Testin does not know was not read as Java", AutomationLanguage.JAVA, AutomationLanguage.of(getProject()));
    }

    // Rule-CODEGEN-117, Rule-CODEGEN-119, Rule-CODEGEN-120
    public void testWorkARobotTestProjectHasNoModuleForIsGrayAndWritesNoJava() {
        interpreterIs("Python SDK");
        final @NotNull TestCase testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        settled();

        assertTrue("Java was written for a Robot Framework test project", generatedClass("nafath.LoginTest").isEmpty());
        final @NotNull AnActionEvent apiModel = theMenuEntry("Testin.ApiModel", testCase);
        assertFalse("Testin API Model from JSON is live on a Robot Framework test project", apiModel.getPresentation().isEnabled());
        assertTrue("Testin API Model from JSON does not say it is not available for Robot Framework yet: " + apiModel.getPresentation().getText(), String.valueOf(apiModel.getPresentation().getText()).endsWith(NOT_YET));

        for (final String actionId : List.of("Testin.AutomateTestCase", "Testin.NavigateToTestMethod", "Testin.RunTestMethod")) {
            assertTrue(actionId + " is gray on a Robot Framework test project, which has a module for it", theMenuEntry(actionId, testCase).getPresentation().isEnabled());
        }
    }
}
