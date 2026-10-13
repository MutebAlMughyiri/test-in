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

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.ui.framework.Spacing;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;

public interface TestCaseSection {
    @NotNull JBPanel<?> getWrapper();

    default boolean isShown() {
        return getWrapper().getParent() != null;
    }

    default void showSection(final @NotNull JBPanel<?> contentPanel) {
        if (!isShown()) contentPanel.add(getWrapper());
        focusOnShow();
    }

    default void focusOnShow() {
        getFocusComponent().requestFocus();
    }

    default boolean accepts() {
        return true;
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    default boolean isShownWith(final @NotNull TestCaseDto testCase) {
        return false;
    }

    // UC-EDITOR-PANEL-006
    default void readyForEditing() {
    }

    // Rule-EDITOR-PANEL-293
    default void addBox(final @NotNull Runnable refit) {
    }

    @NotNull TestCaseDto applyTo(final @NotNull TestCaseDto testCase);

    void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable repackAction);

    // Rule-INTERNAL-097
    default void enableMultiLine(final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable onSave) {
    }

    @NotNull JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final @NotNull TestCaseDto testCase);

    default void styleField(final @NotNull EditorTextField field, final @NotNull String placeholder) {
        DialogStyle.asField(field);
        field.setPlaceholder(placeholder);
        field.setShowPlaceholderWhenFocused(true);
    }

    default @NotNull JBPanel<?> createWrapper(final @NotNull Icon icon, final @NotNull JComponent field) {
        return createWrapper(new JBLabel(icon), field);
    }

    default @NotNull JBPanel<?> createWrapper(final @NotNull JBLabel iconLabel, final @NotNull JComponent field) {
        final @NotNull JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        iconLabel.setBorder(JBUI.Borders.empty(0, Spacing.L, 0, Spacing.M));
        iconPanel.add(iconLabel);

        final @NotNull JBPanel<?> wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(iconPanel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.setBorder(JBUI.Borders.emptyTop(Spacing.M));
        return wrapper;
    }
}