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
import org.testin.model.node.NodeChain;
import org.testin.model.TestCase;
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

    private static @NotNull TestSetNode testSet(final String... treePath) {
        final TestSetNode set = NodeChain.testSetAt(treePath);
        set.setPath(Path.of("C:", "Testin", "test-01", "Test Cases", set.getName()));

        return set;
    }

    private static @NotNull TestCase testCase(final String description, final TestSetNode in) {
        final TestCase testCase = TestCase.builder().id(UUID.randomUUID()).description(description).build();
        testCase.setParent(in);

        return testCase;
    }

    @Test
    public void aTestCasePointsAtTheTestSetThatHoldsIt() {
        final TestSetNode login = testSet("test-01", "Test Cases", "Login");
        final TestCase testCase = testCase("Sign in with a valid user", login);

        final Hit hit = Hit.of(testCase);

        assertSame(hit.node(), login, "a test case is not a node of its own, so the tree goes to its set");
        assertEquals(hit.testCase(), Optional.of(testCase), "and the editor lands on the test case");
        assertEquals(hit.name(), "Sign in with a valid user");
    }

    @Test
    public void aNodeCarriesItselfAndNoTestCase() {
        final TestSetNode login = testSet("test-01", "Test Cases", "Login");

        final Hit hit = Hit.of(login);

        assertSame(hit.node(), login);
        assertTrue(hit.testCase().isEmpty(), "a node is selected and nothing more");
        assertEquals(hit.name(), "Login");
    }

    @Test
    public void everyHitSaysWhereItLives() {
        final TestSetNode login = testSet("test-01", "Test Cases", "Auth", "Login");

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
    public void matchingIgnoresCapitals() {
        assertTrue(GlobalSearch.contains("Sign in with a VALID user", "valid"));
        assertTrue(GlobalSearch.contains("Sign in", "SIGN"));
        assertFalse(GlobalSearch.contains("Sign in", "signed"));
    }

    @Test
    public void testCasesThatMatchOnTheirDescriptionComeFirst() {
        final TestSetNode set = testSet("test-01", "Login");
        final TestCase onADescription = testCase("Login with a valid user", set);
        final TestCase onSomethingUnseen = testCase("Check the balance", set);

        final List<TestCase> sorted = new ArrayList<>(List.of(onSomethingUnseen, onADescription));
        sorted.sort(GlobalSearch.byDescriptionMatchThenText("login"));

        assertSame(sorted.getFirst(), onADescription,
                "the other one is a real hit - it matched on a step - but it looks unrelated at a glance");
    }

    @Test
    public void testCasesMatchingEquallyWellAreAlphabetical() {
        final TestSetNode set = testSet("test-01", "Login");
        final TestCase second = testCase("Login with a valid user", set);
        final TestCase first = testCase("Login fails on a bad password", set);

        final List<TestCase> sorted = new ArrayList<>(List.of(second, first));
        sorted.sort(GlobalSearch.byDescriptionMatchThenText("login"));

        assertSame(sorted.getFirst(), first);
    }

    @Test
    public void theNodeWhoseNameIsTheQueryOutranksOneThatMerelyContainsIt() {
        final TestSetNode exact = testSet("test-01", "Login");
        final TestSetNode longer = testSet("test-01", "Login and logout journeys");

        assertTrue(GlobalSearch.byClosestName(exact, longer) < 0,
                "typing a set's name should put that set at the top, not a sentence containing it");
    }

    @Test
    public void nodesOfTheSameLengthAreAlphabetical() {
        assertTrue(GlobalSearch.byClosestName(testSet("x", "Alpha"), testSet("x", "Bravo")) < 0);
    }

    @Test
    public void aTestSetIsSomewhereToGo() {
        assertTrue(testSet("test-01", "Login").isOpenableInEditor(),
                "the empty-query list is built from this, so a set that stopped saying yes would vanish from it");
    }

    @Test
    public void aPackageIsNot() {
        final TestSetPackageNode testSetPackage = TestSetPackageNode.builder().build();
        testSetPackage.setName("Auth");

        assertFalse(testSetPackage.isOpenableInEditor(), "there is no editor to open, so Enter on it would do nothing");
    }

    @Test
    public void theEmptyQueryListReadsAsTheTreeDoes() {
        final TestSetNode underAuthentication = testSet("test-01", "Test Cases", "Auth", "Login");
        final TestSetNode underBilling = testSet("test-01", "Test Cases", "Billing", "Invoices");

        assertTrue(GlobalSearch.inTreeOrder(underAuthentication, underBilling) < 0,
                "Auth before Billing, so a project's sets stay together instead of interleaving alphabetically");
    }

    @Test
    public void twoNodesInOnePlaceAreAlphabetical() {
        final TestSetNode second = testSet("test-01", "Test Cases", "Beta");
        final TestSetNode first = testSet("test-01", "Test Cases", "Alpha");

        assertTrue(GlobalSearch.inTreeOrder(first, second) < 0);
    }

    // Rule-INTERNAL-002
    @Test
    public void onlyTestSetsAndTestRunsHaveAnEditorToOpen() {
        assertTrue(testSet("test-01", "Login").isOpenableInEditor(), "a test set opens the test set editor");
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
