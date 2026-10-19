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

package org.testin.testcase.create;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.DescriptionAsMethod;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.EditorFields;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DescriptionSection extends AbstractOneLineSection {
    private static final @NotNull Icon REFUSED = Icons.fieldLetter(TestSetEditorFields.DESCRIPTION.getLetter(), Icons.RED);

    private final @NotNull Project project;

    private final @NotNull TextAttributes hint = new TextAttributes();

    private @NotNull Supplier<List<TestCase>> siblings = List::of;

    public DescriptionSection(final @NotNull Project project) {
        super(EditorFields.createCompletionField(project, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(project, TestCaseCompletions.class).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), ""),
                CreateTestCaseFields.DESCRIPTION, Shortcuts.CreateTestCaseDescription);

        this.project = project;
        field.addSettingsProvider(editor -> editor.setPlaceholderAttributes(hint));
    }

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-079
    public void compareAgainst(final @NotNull Supplier<List<TestCase>> siblings) {
        this.siblings = siblings;
    }

    private @NotNull Set<String> takenMethodKeys() {
        return siblings.get().stream()
                .flatMap(sibling -> DescriptionAsMethod.keyOf(sibling.getDescription()).stream())
                .collect(Collectors.toSet());
    }

    // UC-EDITOR-PANEL-005
    public void setError(final boolean error) {
        field.setForeground(error ? Icons.RED : UIUtil.getTextFieldForeground());
        hint.setForegroundColor(error ? Icons.RED : null);
        icon.setIcon(error ? REFUSED : CreateTestCaseFields.DESCRIPTION.getIcon());
        if (error) field.requestFocus();
        field.repaint();
    }

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-079
    @Override
    public boolean accepts() {
        final @NotNull String description = field.getText().trim();
        final @NotNull Optional<DescriptionAsMethod> refused = DescriptionAsMethod.refusalOf(description, takenMethodKeys());

        setError(refused.isPresent());
        refused.ifPresent(refusal -> refusal.say(project, description));
        return refused.isEmpty();
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public @NotNull TestCase applyTo(final @NotNull TestCase testCase) {
        return testCase.edit().description(typed()).build();
    }

    public @NotNull String typed() {
        return field.getText().trim();
    }

    @Override
    public void fillData(final @NotNull TestCase testCase) {
        field.setText(testCase.getDescription());
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    @Override
    public boolean isShownWith(final @NotNull TestCase testCase) {
        return true;
    }
}