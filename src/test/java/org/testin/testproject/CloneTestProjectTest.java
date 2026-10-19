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

package org.testin.testproject;

import org.testin.model.NodeType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CloneTestProjectTest {

    // Rule-TREE-PANEL-107
    @Test
    public void everyFormOfAddressGivesTheRepositoryNameInCapitalsWithoutTestCases() {
        assertEquals(CloneTestProject.repositoryName("https://github.com/acme/nafath-test-cases.git"), "NAFATH");
        assertEquals(CloneTestProject.repositoryName("git@github.com:acme/nafath-test-cases.git"), "NAFATH");
        assertEquals(CloneTestProject.repositoryName("ssh://git@host:2222/acme/nafath-test-cases.git"), "NAFATH");
        assertEquals(CloneTestProject.repositoryName("https://host/acme/nafath-test-cases/"), "NAFATH");
        assertEquals(CloneTestProject.repositoryName("https://host/acme/nafath-test-cases"), "NAFATH");
        assertEquals(CloneTestProject.repositoryName("  https://host/acme/Checkout.GIT  "), "CHECKOUT");
        assertEquals(CloneTestProject.repositoryName("https://host/acme/checkout?ref=main#readme"), "CHECKOUT");
    }

    @Test
    public void anAccountInTheAddressIsNotPartOfTheName() {
        assertEquals(CloneTestProject.repositoryName("https://muteb@bitbucket.org/acme/tests.git"), "TESTS");
    }

    // Rule-TREE-PANEL-107
    @Test
    public void aNameJavaRefusesIsMadeIntoOneItAccepts() {
        assertTrue(NodeType.TEST_PROJECT.canTakeName(CloneTestProject.repositoryName("https://github.com/acme/new.git")),
                "new is a word Java keeps for itself");
        assertTrue(NodeType.TEST_PROJECT.canTakeName(CloneTestProject.repositoryName("https://github.com/acme/nafath-test-cases.git")),
                "and a name it already accepts is kept as it is");
    }
}
