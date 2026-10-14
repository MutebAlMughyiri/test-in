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
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.NameSanitizer;
import org.testin.util.Shortcuts;
import org.testin.util.EditorFields;

import javax.swing.Icon;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DescriptionSection extends AbstractOneLineSection {
    private static final @NotNull Icon REFUSED = Icons.fieldLetter(CreateTestCaseFields.DESCRIPTION.getIcon().letter(), Icons.RED);

    private final @NotNull Project project;

    private final @NotNull Notifier notifier;

    private final @NotNull TextAttributes hint = new TextAttributes();

    private @NotNull Supplier<List<TestCase>> siblings = List::of;

    public DescriptionSection(final @NotNull Project project) {
        super(EditorFields.createCompletionField(project, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(project, TestCaseCompletions.class).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), ""),
                CreateTestCaseFields.DESCRIPTION, Shortcuts.CreateTestCaseDescription);

        this.project = project;
        this.notifier = Services.getInstance(project, Notifier.class);
        field.addSettingsProvider(editor -> editor.setPlaceholderAttributes(hint));
    }

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-001
    public void compareAgainst(final @NotNull Supplier<List<TestCase>> siblings) {
        this.siblings = siblings;
    }

    private @NotNull Set<String> takenMethodKeys() {
        return siblings.get().stream()
                .map(TestCase::getDescription)
                .map(NameSanitizer::methodName)
                .filter(name -> !name.isEmpty())
                .map(NameSanitizer::methodKey)
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

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-001
    @Override
    public boolean accepts() {
        final @NotNull String description = field.getText().trim();
        if (description.isEmpty()) {
            setError(false);
            return true;
        }

        final @NotNull String methodName = NameSanitizer.methodName(description);

        if (NameSanitizer.cannotMakeMethodName(description)) {
            setError(true);
            notifier.softRefuse(project,
                    Bundle.message("description.not.a.method.title"),
                    Bundle.message("description.not.a.method.message", methodName));

            return false;
        }

        if (takenMethodKeys().contains(NameSanitizer.methodKey(methodName))) {
            setError(true);
            notifier.softRefuse(project,
                    Bundle.message("description.taken.title"),
                    Bundle.message("description.taken.message", methodName));

            return false;
        }

        setError(false);
        return true;
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