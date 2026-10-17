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
package org.testin.testrun.form;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.ui.dialogs.CollapsiblePanel;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.DialogHost;
import org.testin.util.Bundle;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestRunConfigurationForm implements DialogComponent {
    private static final boolean EXPANDED = true;

    private static final int COLUMN_GAP = 12;

    private final @NotNull JBPanel<?> wrapper;

    private final @NotNull TestRunNameSection testRunName;

    private final @NotNull ChangeLogSection changeLog;

    private final @NotNull Map<TestRunConfiguration, ChoiceSection> choices = new EnumMap<>(TestRunConfiguration.class);

    private final @NotNull List<TestRunSection> sections = new ArrayList<>();

    private final @NotNull List<List<DialogPart<?>>> rows = new ArrayList<>();

    private @NotNull Runnable answered = () -> {
    };

    // UC-TREE-PANEL-021, UC-TREE-PANEL-022
    public TestRunConfigurationForm(final @NotNull Project project, final @NotNull String name, final @NotNull Map<TestRunConfiguration, String> answers) {
        testRunName = TestRunNameSection.of(name);
        changeLog = ChangeLogSection.of(project, answers.getOrDefault(TestRunConfiguration.CHANGE_LOG, ""));

        final @NotNull CommitSection commit = CommitSection.of(answers.getOrDefault(TestRunConfiguration.COMMIT, ""));

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (!field.isChoice()) continue;

            choices.put(field, ChoiceSection.of(field, answers.getOrDefault(field, field.getDefaultAnswer()), this::answerChanged));
        }

        sections.add(changeLog);
        sections.add(commit);
        sections.addAll(choices.values());

        rows.add(List.of(testRunName.component(), commit.component()));
        rows.add(List.of(changeLog.component()));
        rows.add(List.of(choice(TestRunConfiguration.TEST_TYPE).component(), choice(TestRunConfiguration.PLATFORM).component()));
        rows.add(List.of(choice(TestRunConfiguration.COMPONENT).component(), choice(TestRunConfiguration.LANGUAGE).component()));
        rows.add(List.of(choice(TestRunConfiguration.BROWSER).component()));
        rows.add(List.of(choice(TestRunConfiguration.DEVICE_TYPE).component()));

        wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(CollapsiblePanel.build(Bundle.message("test.run.form.section"), stacked(), EXPANDED), BorderLayout.CENTER);

        applyRules();
    }

    private static @NotNull JComponent laidOut(final @NotNull List<DialogPart<?>> row) {
        if (row.size() == 1) return row.getFirst().getComponent().getPanel();

        final @NotNull JBPanel<?> line = new JBPanel<>(new GridLayout(1, row.size(), JBUI.scale(COLUMN_GAP), 0));
        line.setOpaque(false);
        row.forEach(component -> line.add(component.getComponent().getPanel()));

        return line;
    }

    private @NotNull ChoiceSection choice(final @NotNull TestRunConfiguration field) {
        return Optional.ofNullable(choices.get(field)).orElseThrow();
    }

    private @NotNull JBPanel<?> stacked() {
        final @NotNull JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        for (final List<DialogPart<?>> row : rows) {
            final @NotNull JComponent shown = laidOut(row);
            shown.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            panel.add(shown);
        }

        return panel;
    }

    public void onAnswerChanged(final @NotNull Runnable answered) {
        this.answered = answered;
    }

    // Rule-TREE-PANEL-121
    public @NotNull Optional<String> unanswered() {
        for (final ChoiceSection section : choices.values()) {
            if (!section.needsAnAnswer()) continue;

            return Optional.of(Bundle.message("test.run.form.unanswered", section.field().getDisplayName()));
        }

        return Optional.empty();
    }

    private void answerChanged() {
        applyRules();
        answered.run();
    }

    // Rule-TREE-PANEL-120, Rule-TREE-PANEL-144
    private void applyRules() {
        choices.values().forEach(section -> section.offerFor(this::chosenIn));
        choices.values().forEach(section -> section.showIf(section.field().isShownFor(this::chosenIn)));

        wrapper.revalidate();
        wrapper.repaint();
    }

    private @NotNull String chosenIn(final @NotNull TestRunConfiguration field) {
        return Optional.ofNullable(choices.get(field)).map(ChoiceSection::value).orElse("");
    }

    public @NotNull Map<TestRunConfiguration, String> configuration() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        sections.forEach(section -> section.applyTo(answers));

        return answers;
    }

    public @NotNull String getTestRunName() {
        return testRunName.name();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return wrapper;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return changeLog.component().getComponent().getFocusComponent();
    }

    // Rule-TREE-PANEL-122
    @Override
    public void hostedBy(final @NotNull DialogHost host, final @NotNull Runnable submit) {
        changeLog.takesLineBreaks(host);
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
