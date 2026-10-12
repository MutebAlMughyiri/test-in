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
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.testcase.TestSetEditorAttributes;
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
        final TestCaseDto matching = TestCaseDto.builder()
                .description("Login succeeds")
                .expectedResult("Dashboard")
                .priority(Priority.HIGH)
                .groups(List.of("Regression"))
                .module("accounts")
                .build();
        final TestCaseDto other = TestCaseDto.builder()
                .description("Logout")
                .priority(Priority.LOW)
                .module("accounts")
                .build();

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(matching, other),
                new FilterAndSortChoice("  LOGIN ", Set.of("Regression"), Set.of(Priority.HIGH), Set.of("accounts"), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING));

        assertEquals(result, List.of(matching));
    }

    @Test
    public void filtersRunItemStatusOnlyWhenRunItemExists() {
        final TestCaseDto passed = TestCaseDto.builder().description("passed").build();
        final TestCaseDto missing = TestCaseDto.builder().description("missing").build();
        final RunItem runItem = RunItem.builder()
                .id(passed.getId())
                .executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))
                .build()
                .showing(Optional.of(passed), Optional.empty(), Optional.empty(), false);

        final Map<UUID, RunItem> recorded = Map.of(passed.getId(), runItem);

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(passed, missing),
                new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(RunItemStatus.PASSED), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING),
                id -> Optional.ofNullable(recorded.get(id)));

        assertEquals(result, List.of(passed));
    }

    @Test
    public void theSearchReadsEveryFieldTheTesterWrites() {
        final TestCaseDto tc = TestCaseDto.builder()
                .description("Log in")
                .module("accounts")
                .testData("admin@example.com")
                .preConditions("The account exists")
                .reference("JIRA-123")
                .groups(List.of("Regression"))
                .build();

        for (final String wanted : List.of("accounts", "admin@example.com", "The account exists", "JIRA-123", "Regression")) {
            final List<TestCaseDto> result = TestCaseFilter.filter(List.of(tc), new FilterAndSortChoice(wanted, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING));

            assertEquals(result, List.of(tc), "the search did not read the field holding '" + wanted + "'");
        }
    }

    @Test
    public void filtersOnTheTestCaseStatus() {
        final TestCaseDto reviewed = TestCaseDto.builder().description("reviewed").status(TestCaseStatus.REVIEWED).build();
        final TestCaseDto pending = TestCaseDto.builder().description("pending").status(TestCaseStatus.PENDING).build();

        final List<TestCaseDto> result = TestCaseFilter.filter(List.of(reviewed, pending), new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(TestCaseStatus.REVIEWED), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING));

        assertEquals(result, List.of(reviewed));
    }

    @Test
    public void aQueryNoFieldHoldsFindsNothing() {
        final TestCaseDto tc = TestCaseDto.builder().description("Log in").build();

        assertEquals(TestCaseFilter.filter(List.of(tc), new FilterAndSortChoice("nothing holds this", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING)), List.of());
    }

    // Rule-EDITOR-PANEL-091
    @Test
    public void theSearchReadsEveryFieldTheFieldsPopupLists() {
        final @NotNull TestSetNode set = new TestSetNode();
        set.setPath2(new ArrayList<>(List.of("Shop", "Checkout")));
        final @NotNull TestCaseDto tc = TestCaseDto.builder()
                .id(UUID.randomUUID())
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
        tc.setParent(set);

        final @NotNull List<String> notFound = new ArrayList<>();
        for (final TestSetEditorAttributes field : TestSetEditorAttributes.values()) {
            final @NotNull String value = field.gridValue(tc);
            if (value.isBlank()) continue;

            if (TestCaseFilter.filter(List.of(tc), new FilterAndSortChoice(value, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING)).isEmpty())
                notFound.add(field.getName() + " = " + value);
        }

        assertEquals(notFound, List.of(), "the search does not read these fields");
    }

    // Rule-EDITOR-PANEL-096
    @Test
    public void choosingNothingInAFilterMatchesEveryTestCase() {
        final @NotNull TestCaseDto high = TestCaseDto.builder().description("one").priority(Priority.HIGH).groups(List.of("Smoke")).module("accounts").status(TestCaseStatus.REVIEWED).build();
        final @NotNull TestCaseDto low = TestCaseDto.builder().description("two").priority(Priority.LOW).module("payments").status(TestCaseStatus.DISABLED).build();
        final @NotNull TestCaseDto bare = TestCaseDto.builder().description("three").build();

        final @NotNull List<TestCaseDto> result = TestCaseFilter.filter(List.of(high, low, bare), new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), SortField.ORDER, SortDirection.ASCENDING), _ -> Optional.empty());

        assertEquals(result, List.of(high, low, bare));
    }

    // Rule-EDITOR-PANEL-260
    @Test
    public void theTestSetFilterKeepsOnlyTestCasesOfTheChosenTestSets() {
        final @NotNull TestSetNode login = TestSetNode.builder().path(Path.of("Login")).build();
        final @NotNull TestSetNode checkout = TestSetNode.builder().path(Path.of("Checkout")).build();
        final @NotNull TestCaseDto inLogin = TestCaseDto.builder().description("Sign in").build();
        final @NotNull TestCaseDto inCheckout = TestCaseDto.builder().description("Pay").build();
        inLogin.setParent(login);
        inCheckout.setParent(checkout);

        final @NotNull List<TestCaseDto> result = TestCaseFilter.filter(List.of(inLogin, inCheckout), new FilterAndSortChoice("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(checkout.getPath()), SortField.ORDER, SortDirection.ASCENDING));

        assertEquals(result, List.of(inCheckout), "a test case outside the chosen test set was kept");
    }
}
