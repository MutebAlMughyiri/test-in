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

package org.testin.clipboard;

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.NodeChain;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.testcase.TestSetEditorFields;
import org.testng.annotations.Test;

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CopyChoiceTest {

    private static @NotNull TestCase writtenInFull() {
        final @NotNull TestSetNode set = NodeChain.testSetAt("shop", "checkout", "Payment");

        final @NotNull TestCase testCase = TestCase.builder()
                .uuid(UUID.randomUUID())
                .description("Refuse an expired card")
                .expectedResult("The payment is declined")
                .steps(List.of("Enter the card", "Press Pay"))
                .priority(Priority.HIGH)
                .reference("JIRA-77")
                .testData("card=4111")
                .preConditions("A basket holds one item")
                .groups(List.of("Regression"))
                .module("payments")
                .status(TestCaseStatus.REVIEWED)
                .build();
        testCase.setParent(set);
        return testCase;
    }

    @Test
    public void noTwoRowsAnswerToTheSameKey() {
        final @NotNull Map<KeyStroke, List<CopyChoice>> byKey = new HashMap<>();

        for (final CopyChoice choice : CopyChoice.values()) {
            byKey.computeIfAbsent(choice.getShortcut().getKey(), _ -> new ArrayList<>()).add(choice);
        }

        final @NotNull List<String> shared = byKey.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .toList();

        assertTrue(shared.isEmpty(),
                "two rows of the copy menu answer to one key, so one of them can never be chosen: " + shared);
    }

    @Test
    public void everyRowButAllDetailsIsNamedByTheFieldItCopies() {
        for (final CopyChoice choice : CopyChoice.values()) {
            if (choice == CopyChoice.ALL_DETAILS) {
                assertTrue(choice.getField().isEmpty(), "ALL_DETAILS copies everything, so it names no single field");
                continue;
            }

            assertTrue(choice.getField().isPresent(), choice + " copies no field, so there is nothing for it to copy");
            assertEquals(choice.getName(), choice.getField().orElseThrow().getName(),
                    choice + " is captioned something other than the field it copies, so the two can drift apart");
        }
    }

    @Test
    public void everyRowPrintsItsKey() {
        assertTrue(Arrays.stream(CopyChoice.values()).noneMatch(choice -> choice.getShortcutText().isBlank()),
                "a copy row has no key to print, so the menu shows a blank where its shortcut goes");
    }

    @Test
    public void theClassNameIsCopiedDotted() {
        final @NotNull TestSetNode set = NodeChain.testSetAt("shop", "checkout", "Payment");

        final @NotNull TestCase testCase = TestCase.builder().description("card is declined").build();
        testCase.setParent(set);

        assertEquals(CopyChoice.FQCN.from(testCase), "shop.checkout.PaymentTest.cardIsDeclined");
    }

    // Rule-EDITOR-PANEL-073, Rule-EDITOR-PANEL-207
    @Test
    public void allDetailsCopiesEveryFieldTheTesterWroteAsNameColonValue() {
        final @NotNull TestCase testCase = writtenInFull();
        final @NotNull List<TestSetEditorFields> written = List.of(
                TestSetEditorFields.DESCRIPTION, TestSetEditorFields.EXPECTED_RESULT, TestSetEditorFields.STEPS,
                TestSetEditorFields.PRIORITY, TestSetEditorFields.REFERENCE, TestSetEditorFields.TEST_DATA,
                TestSetEditorFields.PRE_CONDITIONS, TestSetEditorFields.GROUP, TestSetEditorFields.MODULE,
                TestSetEditorFields.STATUS);

        final @NotNull String copied = CopyChoice.ALL_DETAILS.from(testCase);

        for (final TestSetEditorFields field : written) {
            assertTrue(copied.contains(field.getName() + ": " + field.gridValue(testCase)), field.getName() + " is not copied as its name, a colon and its value: " + copied);
        }
        for (final TestSetEditorFields field : TestSetEditorFields.values()) {
            if (written.contains(field)) continue;

            assertFalse(copied.contains(field.getName() + ": "), field.getName() + " is not a field the tester wrote, yet All Details copied it: " + copied);
        }
    }

    // Rule-EDITOR-PANEL-207
    @Test
    public void aFieldLeftEmptyIsNotALine() {
        final @NotNull TestCase testCase = writtenInFull();
        testCase.setReference("");

        assertFalse(CopyChoice.ALL_DETAILS.from(testCase).contains(TestSetEditorFields.REFERENCE.getName() + ":"), "an empty reference still got a line");
    }

    // Rule-EDITOR-PANEL-074
    @Test
    public void severalTestCasesAreSeparatedByABlankLine() {
        final @NotNull TestCase first = TestCase.builder().description("Log in").build();
        final @NotNull TestCase second = TestCase.builder().description("Log out").build();

        assertEquals(CopyChoice.DESCRIPTION.from(List.of(first, second)), """
                Log in
                
                Log out""");
    }

    // Rule-EDITOR-PANEL-208
    @Test
    public void aSingleValueIsCopiedBareAndSoAreTheValuesTheTesterNeverWrites() {
        final @NotNull TestCase testCase = writtenInFull();

        assertEquals(CopyChoice.DESCRIPTION.from(testCase), "Refuse an expired card", "a single value carries no caption");
        assertEquals(CopyChoice.UUID.from(testCase), testCase.getUuid().toString(), "the identity can be copied on its own");
        assertEquals(CopyChoice.FQCN.from(testCase), "shop.checkout.PaymentTest.refuseAnExpiredCard", "and the class name");
        assertEquals(CopyChoice.PATH.from(testCase), TestSetEditorFields.PATH.gridValue(testCase), "and the path");
    }
}
