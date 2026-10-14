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

package org.testin.editor.card;

import com.google.common.collect.ImmutableList;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.codegen.CodeOn;
import org.testin.editor.TestinEditor;
import org.testin.model.Automated;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.navigate.NavigateToTestCaseAction;
import org.testin.navigate.NavigateToTestMethodAction;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.runner.ExecuteTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Icons;

import javax.swing.Icon;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
public enum TestCaseQuickAction {
    NAVIGATE_TO_TEST_METHOD(
            Bundle.message("action.Testin.NavigateToTestMethod.text"),
            "Testin.NavigateToTestMethod",
            ImmutableList.of(OptionalPlugin.JAVA),
            Icons.TEST_CASE,
            (project, testCases) -> NavigateToTestMethodAction.execute(project, testCases.getFirst()),
            (_, _) -> Optional.empty()
    ),

    RUN_TEST_METHOD(
            Bundle.message("action.Testin.RunTestMethod.text"),
            "Testin.RunTestMethod",
            ImmutableList.of(OptionalPlugin.JAVA, OptionalPlugin.TESTNG),
            AllIcons.RunConfigurations.TestState.Run,
            ExecuteTestCases::run,
            (shownIn, _) -> shownIn.flatMap(AbstractNode::whySignedOff)
    ),

    STOP_TEST_METHOD(
            Bundle.message("card.stop.test.method"),
            "",
            ImmutableList.of(OptionalPlugin.TESTNG),
            AllIcons.Actions.Suspend,
            TestCaseQuickAction::stopExecution,
            (_, _) -> Optional.empty()
    ),

    NAVIGATE_TO_TEST_CASE(
            Bundle.message("action.Testin.NavigateToTestCase.text"),
            "Testin.NavigateToTestCase",
            ImmutableList.of(),
            Icons.TEST_CASE_LETTER,
            (project, testCases) -> NavigateToTestCaseAction.execute(project, testCases.getFirst()),
            (_, testCase) -> NavigateToTestCaseAction.whyNot(testCase)
    );

    private final @NotNull String tooltip;
    private final @NotNull String actionId;
    @Getter(AccessLevel.NONE)
    private final @NotNull ImmutableList<OptionalPlugin> requires;

    private final @NotNull Icon icon;

    @Getter(AccessLevel.NONE)
    private final @NotNull BiConsumer<Project, List<TestCase>> onClick;

    @Getter(AccessLevel.NONE)
    private final @NotNull BiFunction<Optional<AbstractNode>, TestCase, Optional<String>> whyNotHere;

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-234, Rule-EDITOR-PANEL-235
    public static @NotNull List<Offered> onCard(final @NotNull Project project, final @NotNull AbstractNode openOn, final @NotNull TestCase testCase) {
        final @NotNull List<TestCaseQuickAction> buttons = openOn.isTestCaseContainer()
                ? List.of(NAVIGATE_TO_TEST_METHOD, RUN_TEST_METHOD)
                : List.of(NAVIGATE_TO_TEST_METHOD, RUN_TEST_METHOD, NAVIGATE_TO_TEST_CASE);

        return buttons.stream()
                .map(button -> button.offer(project, Optional.of(openOn), testCase))
                .toList();
    }

    // UC-EDITOR-PANEL-043
    private static void stopExecution(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final int stopped = Services.getInstance(project, TestNGExecution.class).stop(testCases);

        if (stopped > 0) Services.getInstance(project, Notifier.class).softShowCounted(project, Done.STOPPED, stopped);
    }

    public static @NotNull TestCaseQuickAction executionSlot(final @NotNull Project project, final @NotNull TestCase testCase) {
        return executionSlot(project, List.of(testCase));
    }

    // UC-EDITOR-PANEL-043
    public static @NotNull TestCaseQuickAction executionSlot(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull TestNGExecution execution = Services.getInstance(project, TestNGExecution.class);

        return testCases.stream().anyMatch(testCase -> execution.isRunning(testCase.getUuid()))
                ? STOP_TEST_METHOD
                : RUN_TEST_METHOD;
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-234, Rule-EDITOR-PANEL-266
    public @NotNull Offered offer(final @NotNull Project project, final @NotNull Optional<AbstractNode> shownIn, final @NotNull TestCase testCase) {
        final @NotNull TestCaseQuickAction now = gestureOn(project, testCase);

        return new Offered(now, now.whyNotOffered(project).or(() -> now.whyNotHere(shownIn, testCase)));
    }

    // Rule-EDITOR-PANEL-266
    public @NotNull Optional<String> whyNotHere(final @NotNull Optional<AbstractNode> shownIn, final @NotNull TestCase testCase) {
        return whyNotHere.apply(shownIn, testCase);
    }

    // UC-EDITOR-PANEL-043
    public @NotNull TestCaseQuickAction gestureOn(final @NotNull Project project, final @NotNull TestCase testCase) {
        return this == RUN_TEST_METHOD ? executionSlot(project, testCase) : this;
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-180
    public void executeFor(final @NotNull TestinEditor editor, final @NotNull TestCase testCase) {
        executeFor(editor.getProject(), Optional.of(editor), List.of(testCase));
    }

    // UC-EDITOR-PANEL-035, UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-180
    public void executeFor(final @NotNull Project project, final @NotNull Optional<TestinEditor> editor, final @NotNull List<TestCase> testCases) {
        if (this == RUN_TEST_METHOD)
            editor.ifPresent(openEditor -> testCases.forEach(testCase -> openEditor.launching(testCase.getUuid())));

        execute(project, testCases);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-005, Rule-CODEGEN-082
    public boolean enableOrExplain(final @NotNull Project project, final @NotNull Presentation presentation) {
        if (!requires.stream().allMatch(plugin -> plugin.enableOrExplain(presentation, tooltip))) return false;

        return whyNotOffered(project).map(reason -> {
            presentation.setEnabled(false);
            presentation.setDescription(reason);

            return false;
        }).orElse(true);
    }

    // UC-EDITOR-PANEL-047, UC-EDITOR-PANEL-048
    public @NotNull Icon iconOn(final @NotNull Automated automation) {
        return this == NAVIGATE_TO_TEST_METHOD ? automation.getIcon() : icon;
    }

    public void execute(final @NotNull Project project, final @NotNull TestCase testCase) {
        execute(project, List.of(testCase));
    }

    public void execute(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        if (testCases.isEmpty()) return;

        onClick.accept(project, testCases);
    }

    @NotNull String hint() {
        return (tooltip + " " + Declared.shortcutText(actionId)).trim();
    }

    // UC-EDITOR-PANEL-047, Rule-CODEGEN-082
    public @NotNull Optional<String> whyNotOffered(final @NotNull Project project) {
        final @NotNull Optional<String> missing = requires.stream()
                .filter(plugin -> !plugin.isAvailable())
                .findFirst()
                .map(plugin -> plugin.needs(tooltip));

        // Rule-CODEGEN-082
        if (missing.isPresent() || !requires.contains(OptionalPlugin.JAVA)) return missing;
        return CodeOn.whyOff(project);
    }
}
