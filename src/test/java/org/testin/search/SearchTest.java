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

package org.testin.search;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SearchTest {

    private static @NotNull TestSetNode testSet(final String name, final String... chain) {
        final TestSetNode set = TestSetNode.builder().build();
        set.setName(name);
        set.setPath(Path.of("C:", "Testin", "test-01", "Test Cases", name));
        set.setPath2(new ArrayList<>(List.of(chain)));

        return set;
    }

    private static @NotNull TestCaseDto testCase(final String description, final TestSetNode in) {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description(description).build();
        tc.setParent(in);

        return tc;
    }

    @Test
    public void aTestCasePointsAtTheTestSetThatHoldsIt() {
        final TestSetNode login = testSet("Login", "test-01", "Test Cases", "Login");
        final TestCaseDto tc = testCase("Sign in with a valid user", login);

        final Hit hit = Hit.of(tc);

        assertSame(hit.node(), login, "a test case is not a node of its own, so the tree goes to its set");
        assertEquals(hit.testCase(), Optional.of(tc), "and the editor lands on the test case");
        assertEquals(hit.name(), "Sign in with a valid user");
    }

    @Test
    public void aNodeCarriesItselfAndNoTestCase() {
        final TestSetNode login = testSet("Login", "test-01", "Test Cases", "Login");

        final Hit hit = Hit.of(login);

        assertSame(hit.node(), login);
        assertTrue(hit.testCase().isEmpty(), "a node is selected and nothing more");
        assertEquals(hit.name(), "Login");
    }

    @Test
    public void everyHitSaysWhereItLives() {
        final TestSetNode login = testSet("Login", "test-01", "Test Cases", "Auth", "Login");

        assertEquals(Hit.of(login).where(), "test-01 > Test Cases > Auth > Login");
        assertEquals(Hit.of(testCase("Sign in", login)).where(), "test-01 > Test Cases > Auth > Login",
                "three test cases can be called the same thing in three sets, so the row has to say which");
    }

    @Test
    public void aQueryOfOneCharacterIsNotAQuery() {
        assertTrue(GlobalSearch.tooShort(""), "an empty field is not a search");
        assertTrue(GlobalSearch.tooShort("a"), "one letter matches almost the whole project");
        assertTrue(GlobalSearch.tooShort("  a  "), "and padding does not make it two");
        assertFalse(GlobalSearch.tooShort("ab"));
    }

    @Test
    public void matchingIgnoresCase() {
        assertTrue(GlobalSearch.contains("Sign in with a VALID user", "valid"));
        assertTrue(GlobalSearch.contains("Sign in", "SIGN"));
        assertFalse(GlobalSearch.contains("Sign in", "signed"));
    }

    @Test
    public void testCasesThatMatchOnTheirDescriptionComeFirst() {
        final TestSetNode set = testSet("Login", "test-01");
        final TestCaseDto onADescription = testCase("Login with a valid user", set);
        final TestCaseDto onSomethingUnseen = testCase("Check the balance", set);

        final List<TestCaseDto> sorted = new ArrayList<>(List.of(onSomethingUnseen, onADescription));
        sorted.sort(GlobalSearch.byDescriptionMatchThenText("login"));

        assertSame(sorted.getFirst(), onADescription,
                "the other one is a real hit - it matched on a step - but it looks unrelated at a glance");
    }

    @Test
    public void testCasesMatchingEquallyWellAreAlphabetical() {
        final TestSetNode set = testSet("Login", "test-01");
        final TestCaseDto second = testCase("Login with a valid user", set);
        final TestCaseDto first = testCase("Login fails on a bad password", set);

        final List<TestCaseDto> sorted = new ArrayList<>(List.of(second, first));
        sorted.sort(GlobalSearch.byDescriptionMatchThenText("login"));

        assertSame(sorted.getFirst(), first);
    }

    @Test
    public void theNodeWhoseNameIsTheQueryOutranksOneThatMerelyContainsIt() {
        final TestSetNode exact = testSet("Login", "test-01");
        final TestSetNode longer = testSet("Login and logout journeys", "test-01");

        assertTrue(GlobalSearch.byClosestName(exact, longer) < 0,
                "typing a set's name should put that set at the top, not a sentence containing it");
    }

    @Test
    public void nodesOfTheSameLengthAreAlphabetical() {
        assertTrue(GlobalSearch.byClosestName(testSet("Alpha", "x"), testSet("Bravo", "x")) < 0);
    }

    @Test
    public void aTestSetIsSomewhereToGo() {
        assertTrue(testSet("Login", "test-01").isOpenableInEditor(),
                "the empty-query list is built from this, so a set that stopped saying yes would vanish from it");
    }

    @Test
    public void aPackageIsNot() {
        final TestSetPackageNode pkg = TestSetPackageNode.builder().build();
        pkg.setName("Auth");

        assertFalse(pkg.isOpenableInEditor(), "there is no editor to open, so Enter on it would do nothing");
    }

    @Test
    public void theEmptyQueryListReadsAsTheTreeDoes() {
        final TestSetNode underAuth = testSet("Login", "test-01", "Test Cases", "Auth", "Login");
        final TestSetNode underBilling = testSet("Invoices", "test-01", "Test Cases", "Billing", "Invoices");

        assertTrue(GlobalSearch.inTreeOrder(underAuth, underBilling) < 0,
                "Auth before Billing, so a project's sets stay together instead of interleaving alphabetically");
    }

    @Test
    public void twoNodesInOnePlaceAreAlphabetical() {
        final TestSetNode second = testSet("Beta", "test-01", "Test Cases");
        final TestSetNode first = testSet("Alpha", "test-01", "Test Cases");

        assertTrue(GlobalSearch.inTreeOrder(first, second) < 0);
    }

    // Rule-INTERNAL-002
    @Test
    public void onlyTestSetsAndTestRunsHaveAnEditorToOpen() {
        assertTrue(testSet("Login", "test-01").isOpenableInEditor(), "a test set opens the test set editor");
        assertTrue(TestRunNode.builder().build().isOpenableInEditor(), "a test run opens the test run editor");
    }

    // Rule-INTERNAL-002
    @Test
    public void aFolderOfNodesHasNoEditorAndIsNotOpened() {
        assertFalse(TestSetPackageNode.builder().build().isOpenableInEditor());
        assertFalse(TestRunPackageNode.builder().build().isOpenableInEditor());
        assertFalse(TestCasesFolderNode.builder().build().isOpenableInEditor(),
                "the Test Cases folder is somewhere to look through, not somewhere to open");
        assertFalse(TestRunsFolderNode.builder().build().isOpenableInEditor(),
                "and the Test Runs folder is the one that crashed");
        assertFalse(new TestProjectNode().isOpenableInEditor(),
                "a project is where the two folders live, and neither it nor they open");
    }
}
