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

package org.testin.model.node;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertEquals;

public class NodeNameUnderTest {

    private static final @NotNull Path TEST_CASES = Path.of("Checkout", "Test Cases");

    // Rule-EDITOR-PANEL-260, Rule-TREE-PANEL-129
    @Test
    public void aTestSetDirectlyUnderTheFolderIsNamedAlone() {
        final @NotNull TestSetNode login = TestSetNode.builder().path(TEST_CASES.resolve("Login")).build();

        assertEquals(login.nameUnder(TEST_CASES), "Login");
    }

    // Rule-EDITOR-PANEL-260, Rule-TREE-PANEL-129
    @Test
    public void aTestSetInAPackageIsNamedByItsPlaceUnderTheFolder() {
        final @NotNull TestSetNode cards = TestSetNode.builder().path(TEST_CASES.resolve("Payments").resolve("Cards")).build();

        assertEquals(cards.nameUnder(TEST_CASES), "Payments / Cards",
                "two test sets of one name in two packages must read apart in the Test Set filter");
    }
}
