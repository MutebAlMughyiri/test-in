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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class GroupsTest {

    // Rule-EDITOR-PANEL-095
    @Test
    public void theGroupListOffersOnlyTheGroupsTheTestCasesCarry() {
        final @NotNull List<TestCase> testCases = List.of(
                TestCase.builder().description("one").groups(new ArrayList<>(List.of("SMOKE", "UI"))).build(),
                TestCase.builder().description("two").groups(new ArrayList<>(List.of(" REGRESSION "))).build(),
                TestCase.builder().description("three").groups(new ArrayList<>(List.of("SMOKE", "   "))).build(),
                TestCase.builder().description("four").build());

        assertEquals(Groups.in(testCases), Set.of("SMOKE", "UI", "REGRESSION"), "a group no test case carries, or a blank one, is never offered");
    }
}
