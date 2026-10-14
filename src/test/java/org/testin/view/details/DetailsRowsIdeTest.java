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
package org.testin.view.details;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.ShownFields;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.view.Drawn;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class DetailsRowsIdeTest extends BasePlatformTestCase {

    private static final @NotNull List<String> PATH = List.of("Demo", "Test Cases", "Login");

    private static final @NotNull List<String> ROWS_IN_ORDER = List.of(
            caption("field.expected.result"),
            caption("field.steps"),
            caption("field.pre.conditions"),
            caption("field.test.data"),
            caption("field.reference"),
            caption("field.module"),
            caption("field.order"),
            caption("details.created"),
            caption("details.updated"));

    private static @NotNull String caption(final @NotNull String key) {
        return Bundle.message(key).toUpperCase(Locale.ROOT);
    }

    private static @NotNull TestCase everyFieldFilled() {
        final @NotNull ZonedDateTime at = ZonedDateTime.of(2026, 9, 13, 14, 14, 0, 0, ZoneOffset.UTC);
        return TestCase.builder()
                .description("Log in with a valid user")
                .expectedResult("The dashboard opens")
                .steps(List.of("Open the login page"))
                .preConditions("A user exists")
                .testData("user = muteb")
                .reference("JIRA-412")
                .module("Accounts")
                .createdBy("muteb").createdAt(at)
                .updatedBy("muteb").updatedAt(at)
                .build();
    }

    private static @NotNull GridBagConstraints placed(final @NotNull Component component) {
        return ((GridBagLayout) component.getParent().getLayout()).getConstraints(component);
    }

    private @NotNull JBPanel<?> drawn(final @NotNull TestCase testCase) {
        return Drawn.detailsTab(getProject(), testCase, Optional.empty(), PATH);
    }

    private @NotNull List<String> words(final @NotNull TestCase testCase) {
        return Drawn.words(drawn(testCase));
    }

    // Rule-VIEW-PANEL-023
    public void testTheRowsAreDrawnInOneFixedOrder() {
        final @NotNull TestCase testCase = everyFieldFilled();
        final @NotNull TestSetNode login = new TestSetNode();
        login.setPath(Path.of(String.join("/", PATH)));
        testCase.setParent(login);
        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        testCases.putTestCaseVerbatim(login.getPath(), testCase);
        try {
            final @NotNull List<String> captions = words(testCase).stream().filter(ROWS_IN_ORDER::contains).toList();

            assertEquals("the test case's rows were drawn out of their fixed order", ROWS_IN_ORDER, captions);
        } finally {
            testCases.removeTestCase(login.getPath(), testCase.getUuid());
        }
    }

    // Rule-VIEW-PANEL-103
    public void testAFieldUntickedInFieldsIsStillDrawn() {
        final @NotNull Set<TestSetEditorFields> was = ShownFields.read(ShownFields.IN_TEST_SETS, TestSetEditorFields.class);
        final @NotNull Set<TestSetEditorFields> chosen = EnumSet.allOf(TestSetEditorFields.class);
        chosen.removeAll(Set.of(TestSetEditorFields.STEPS, TestSetEditorFields.MODULE));
        ShownFields.write(ShownFields.IN_TEST_SETS, chosen);
        try {
            final @NotNull List<String> words = Drawn.words(Drawn.detailsTab(getProject(), everyFieldFilled(), Optional.empty(), PATH));

            assertTrue("a row unticked in Fields went missing from the Details tab: " + words, words.contains(caption("field.steps")));
            assertTrue("a row unticked in Fields went missing from the Details tab: " + words, words.contains(caption("field.module")));
        } finally {
            ShownFields.write(ShownFields.IN_TEST_SETS, was);
        }
    }

    // Rule-VIEW-PANEL-006, Rule-VIEW-PANEL-024
    public void testAnEmptyFieldIsNotDrawnAndNeitherIsItsCaption() {
        final @NotNull List<String> words = words(everyFieldFilled().toBuilder().preConditions("").testData(" ").reference("").build());

        assertFalse("an empty pre conditions row was drawn: " + words, words.contains(caption("field.pre.conditions")));
        assertFalse("a blank test data row was drawn: " + words, words.contains(caption("field.test.data")));
        assertFalse("an empty reference row was drawn: " + words, words.contains(caption("field.reference")));
        assertTrue("a filled field went missing with the empty ones: " + words, words.contains(caption("field.module")));
    }

    // Rule-VIEW-PANEL-025
    public void testATestCaseWithNoDescriptionShowsADashForItsTitle() {
        final @NotNull List<String> words = words(everyFieldFilled().toBuilder().description("").build());

        assertTrue("a test case with no description has no dash for its title: " + words, words.contains("-"));
    }

    // Rule-VIEW-PANEL-027
    public void testABlankStepIsSkippedAndItsNumberIsNotGivenToTheNextStep() {
        final @NotNull List<String> words = words(everyFieldFilled().toBuilder().steps(List.of("Open the login page", "Type the password", " ", "Press log in")).build());

        assertTrue("the first step was not drawn: " + words, words.contains("1- Open the login page."));
        assertTrue("the second step was not drawn: " + words, words.contains("2- Type the password."));
        assertFalse("the blank third step was drawn: " + words, words.stream().anyMatch(word -> word.startsWith("3-")));
        assertTrue("the step after the blank one was renumbered: " + words, words.contains("4- Press log in."));
    }

    // Rule-VIEW-PANEL-082
    public void testACaptionSitsOnItsOwnLineAboveItsValue() {
        final @NotNull JBPanel<?> tab = drawn(everyFieldFilled());
        final @NotNull Component caption = Drawn.reading(tab, caption("field.expected.result"));
        final @NotNull Component value = Drawn.reading(tab, "The dashboard opens.");

        assertSame("the caption and its value are not rows of the same panel", caption.getParent(), value.getParent());
        assertEquals("the value is not on the line under its caption", placed(caption).gridy + 1, placed(value).gridy);
        assertEquals("the value does not start where its caption does", placed(caption).gridx, placed(value).gridx);
        assertEquals("the caption is not in the caption font", Fonts.panelCaption(), caption.getFont());
    }
}
