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

package org.testin.filter;

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.NodeChain;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.testcase.TestSetEditorFields;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestCaseFilterTest {

    @Test
    public void filtersSearchAndMetadataTogether() {
        final TestCase matching = TestCase.builder()
                .description("Login succeeds")
                .expectedResult("Dashboard")
                .priority(Priority.HIGH)
                .groups(List.of("Regression"))
                .module("accounts")
                .build();
        final TestCase other = TestCase.builder()
                .description("Logout")
                .priority(Priority.LOW)
                .module("accounts")
                .build();

        final List<TestCase> result = TestCaseFilter.filter(
                List.of(matching, other),
                new FilterAndSortChoice("  LOGIN ", Set.of("Regression"), Set.of(Priority.HIGH), Set.of("accounts"), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING));

        assertEquals(result, List.of(matching));
    }

    @Test
    public void filtersRunItemStatusOnlyWhenRunItemExists() {
        final TestCase passed = TestCase.builder().description("passed").build();
        final TestCase missing = TestCase.builder().description("missing").build();
        final RunItem runItem = RunItem.builder()
                .uuid(passed.getUuid())
                .executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))
                .build()
                .showing(Optional.of(passed), Optional.empty(), Optional.empty(), false);

        final Map<UUID, RunItem> recorded = Map.of(passed.getUuid(), runItem);

        final List<TestCase> result = TestCaseFilter.filter(
                List.of(passed, missing),
                new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(RunItemStatus.PASSED), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING),
                testCaseUuid -> Optional.ofNullable(recorded.get(testCaseUuid)));

        assertEquals(result, List.of(passed));
    }

    @Test
    public void theSearchReadsEveryFieldTheTesterWrites() {
        final TestCase testCase = TestCase.builder()
                .description("Log in")
                .module("accounts")
                .testData("admin@example.com")
                .preConditions("The account exists")
                .reference("JIRA-123")
                .groups(List.of("Regression"))
                .build();

        for (final String wanted : List.of("accounts", "admin@example.com", "The account exists", "JIRA-123", "Regression")) {
            final List<TestCase> result = TestCaseFilter.filter(List.of(testCase), new FilterAndSortChoice(wanted, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING));

            assertEquals(result, List.of(testCase), "the search did not read the field holding '" + wanted + "'");
        }
    }

    @Test
    public void filtersOnTheTestCaseStatus() {
        final TestCase reviewed = TestCase.builder().description("reviewed").status(TestCaseStatus.REVIEWED).build();
        final TestCase pending = TestCase.builder().description("pending").status(TestCaseStatus.PENDING).build();

        final List<TestCase> result = TestCaseFilter.filter(List.of(reviewed, pending), new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(TestCaseStatus.REVIEWED), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING));

        assertEquals(result, List.of(reviewed));
    }

    @Test
    public void aQueryNoFieldHoldsFindsNothing() {
        final TestCase testCase = TestCase.builder().description("Log in").build();

        assertEquals(TestCaseFilter.filter(List.of(testCase), new FilterAndSortChoice("nothing holds this", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING)), List.of());
    }

    // Rule-EDITOR-PANEL-091
    @Test
    public void theSearchReadsEveryFieldTheFieldsPopupLists() {
        final @NotNull TestSetNode set = NodeChain.testSetAt("Shop", "Checkout");
        final @NotNull TestCase testCase = TestCase.builder()
                .uuid(UUID.randomUUID())
                .description("Refuse an expired card")
                .expectedResult("The payment is declined")
                .steps(List.of("Enter the card number"))
                .priority(Priority.HIGH)
                .reference("JIRA-77")
                .testData("card=4111")
                .preConditions("A basket holds one item")
                .groups(List.of("Regression"))
                .module("payments")
                .status(TestCaseStatus.REVIEWED)
                .createdBy("Sara")
                .updatedBy("Omar")
                .createdAt(ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC))
                .updatedAt(ZonedDateTime.of(2026, 6, 7, 8, 9, 10, 0, ZoneOffset.UTC))
                .build();
        testCase.setParent(set);

        final @NotNull List<String> notFound = new ArrayList<>();
        for (final TestSetEditorFields field : TestSetEditorFields.values()) {
            final @NotNull String value = field.gridValue(testCase);
            if (value.isBlank()) continue;

            if (TestCaseFilter.filter(List.of(testCase), new FilterAndSortChoice(value, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING)).isEmpty())
                notFound.add(field.getName() + " = " + value);
        }

        assertEquals(notFound, List.of(), "the search does not read these fields");
    }

    // Rule-EDITOR-PANEL-096
    @Test
    public void choosingNothingInAFilterMatchesEveryTestCase() {
        final @NotNull TestCase high = TestCase.builder().description("one").priority(Priority.HIGH).groups(List.of("Smoke")).module("accounts").status(TestCaseStatus.REVIEWED).build();
        final @NotNull TestCase low = TestCase.builder().description("two").priority(Priority.LOW).module("payments").status(TestCaseStatus.DISABLED).build();
        final @NotNull TestCase bare = TestCase.builder().description("three").build();

        final @NotNull List<TestCase> result = TestCaseFilter.filter(List.of(high, low, bare), new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING), _ -> Optional.empty());

        assertEquals(result, List.of(high, low, bare));
    }

    // Rule-EDITOR-PANEL-260
    @Test
    public void theTestSetFilterKeepsOnlyTestCasesOfTheChosenTestSets() {
        final @NotNull TestSetNode login = TestSetNode.builder().path(Path.of("Login")).build();
        final @NotNull TestSetNode checkout = TestSetNode.builder().path(Path.of("Checkout")).build();
        final @NotNull TestCase inLogin = TestCase.builder().description("Sign in").build();
        final @NotNull TestCase inCheckout = TestCase.builder().description("Pay").build();
        inLogin.setParent(login);
        inCheckout.setParent(checkout);

        final @NotNull List<TestCase> result = TestCaseFilter.filter(List.of(inLogin, inCheckout), new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(checkout.getPath()), SortField.ORDER, SortDirection.ASCENDING));

        assertEquals(result, List.of(inCheckout), "a test case outside the chosen test set was kept");
    }
}
