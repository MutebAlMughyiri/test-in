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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class ModulesTest {

    // Rule-EDITOR-PANEL-095
    @Test
    public void theModuleListOffersOnlyTheModulesTheTestCasesCarry() {
        final @NotNull List<TestCaseDto> testCases = List.of(
                TestCaseDto.builder().description("one").module("accounts").build(),
                TestCaseDto.builder().description("two").module(" payments ").build(),
                TestCaseDto.builder().description("three").module("accounts").build(),
                TestCaseDto.builder().description("four").module("   ").build());

        assertEquals(Modules.in(testCases), Set.of("accounts", "payments"), "a module no test case carries, or a blank one, is never offered");
    }
}
