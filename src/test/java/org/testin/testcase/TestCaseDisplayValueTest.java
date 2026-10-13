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

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class TestCaseDisplayValueTest {

    private static final @NotNull TestCase TYPED = TestCase.builder()
            .description("log in with a valid user")
            .expectedResult("the dashboard opens")
            .steps(List.of("open the login page"))
            .preConditions("a user exists")
            .reference("jira-412")
            .module("accounts")
            .testData("user = muteb")
            .build();

    // Rule-VIEW-PANEL-026
    @Test
    public void theFourProseRowsReadCapitalizedAndClosed() {
        assertEquals(TestSetEditorFields.DESCRIPTION.displayValue(TYPED), "Log in with a valid user.");
        assertEquals(TestSetEditorFields.EXPECTED_RESULT.displayValue(TYPED), "The dashboard opens.");
        assertEquals(TestSetEditorFields.STEPS.displayValue(TYPED), "Open the login page.");
        assertEquals(TestSetEditorFields.PRE_CONDITIONS.displayValue(TYPED), "A user exists.");
    }

    // Rule-VIEW-PANEL-026
    @Test
    public void referenceModuleAndTestDataReadAsTheTesterTypedThem() {
        assertEquals(TestSetEditorFields.REFERENCE.displayValue(TYPED), "jira-412", "a reference is an identifier");
        assertEquals(TestSetEditorFields.MODULE.displayValue(TYPED), "accounts", "a module is a label");
        assertEquals(TestSetEditorFields.TEST_DATA.displayValue(TYPED), "user = muteb");
    }
}
