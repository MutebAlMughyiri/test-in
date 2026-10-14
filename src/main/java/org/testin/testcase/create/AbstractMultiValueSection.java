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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.ui.framework.Spacing;
import org.testin.util.EditorFields;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// UC-EDITOR-PANEL-005
public abstract class AbstractMultiValueSection implements TestCaseSection {
    protected final @NotNull Project project;

    private final @NotNull TestCaseCompletions testCaseValues;

    @Getter
    private final @NotNull List<EditorTextField> fields = new ArrayList<>();

    private final @NotNull JBPanel<?> container;

    private final @NotNull JBPanel<?> wrapper;

    protected AbstractMultiValueSection(final @NotNull Project project) {
        this.project = project;
        this.testCaseValues = Services.getInstance(project, TestCaseCompletions.class);

        this.container = new JBPanel<>();
        this.container.setLayout(new BoxLayout(this.container, BoxLayout.Y_AXIS));
        this.container.setOpaque(false);

        this.wrapper = createWrapper(field().getIcon(), this.container);
    }

    protected abstract @NotNull CreateTestCaseFields field();

    protected abstract @NotNull Set<String> completions(final @NotNull TestCaseCompletions cache);

    protected abstract @NotNull List<String> valuesOf(final @NotNull TestCase testCase);

    protected abstract @NotNull TestCase write(final @NotNull TestCase testCase, final @NotNull List<String> values);

    protected @NotNull String placeholderFor(final int index) {
        return field().getPlaceholder();
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    // Rule-INTERNAL-057
    @Override
    public void focusOnShow() {
        if (!fields.isEmpty()) fields.getLast().requestFocus();
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-293
    public void showSection(final @NotNull JBPanel<?> contentPanel, final @NotNull Runnable repackAction) {
        showSection(contentPanel);
        wrapper.setVisible(true);
        readyForEditing();
        refitAndFocus(repackAction);
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-293
    @Override
    public void addBox(final @NotNull Runnable refit) {
        addField("");
        refitAndFocus(refit);
    }

    private void refitAndFocus(final @NotNull Runnable refit) {
        ApplicationManager.getApplication().invokeLater(() -> {
            refit.run();
            focusOnShow();
        });
    }

    public void addField(final @NotNull String text) {
        final @NotNull EditorTextField box = EditorFields.createCompletionField(project,
                new TextFieldWithAutoCompletion.StringsCompletionProvider(completions(testCaseValues), field().getIcon()), text);

        box.setOneLineMode(true);
        DialogStyle.asChoice(box);
        box.setPlaceholder(placeholderFor(fields.size()));
        box.setShowPlaceholderWhenFocused(true);
        box.setBorder(JBUI.Borders.empty(Spacing.S, Spacing.L));

        final @NotNull JBPanel<?> row = new JBPanel<>(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(JBUI.Borders.emptyBottom(Spacing.S));
        row.add(box, BorderLayout.CENTER);

        fields.add(box);
        container.add(row);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-033
    @Override
    public @NotNull TestCase applyTo(final @NotNull TestCase testCase) {
        final @NotNull List<String> values = new ArrayList<>();

        for (final EditorTextField box : fields) {
            final @NotNull String typed = box.getText().trim();
            if (!typed.isEmpty()) values.add(typed);
        }

        return write(testCase, values);
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable repackAction) {
        base.registerShortcut(mainPanel, field().getShortcut().getCustomShortcut(), () -> showSection(slot, repackAction));
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return fields.isEmpty() ? container : fields.getLast();
    }

    @Override
    public void setEditable(final boolean editable) {
        fields.forEach(box -> box.setEnabled(editable));
    }

    public void setData(final @NotNull List<String> values) {
        container.removeAll();
        fields.clear();

        values.forEach(this::addField);
    }

    @Override
    public void fillData(final @NotNull TestCase testCase) {
        setData(valuesOf(testCase));
    }

    // UC-EDITOR-PANEL-006
    @Override
    public void readyForEditing() {
        if (fields.isEmpty()) addField("");
    }
}
