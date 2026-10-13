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

package org.testin.lightmode;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.framework.Prose;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Optional;

class TestCaseDetails extends JBPanel<TestCaseDetails> {
    static final int GAP = 10;

    private @NotNull Optional<TestCaseDto> shown = Optional.empty();

    private float zoom = 1.0f;

    private boolean cutOff = false;

    TestCaseDetails() {
        super(new GridBagLayout());
        setOpaque(false);

    }

    void show(final @NotNull TestCaseDto testCase) {
        shown = Optional.of(testCase);

        render();
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217
    void setCutOff(final boolean truncated) {
        if (cutOff == truncated) return;

        cutOff = truncated;
        render();
    }

    void setZoom(final float zoom) {
        if (this.zoom == zoom) return;

        this.zoom = zoom;
        render();
    }

    private void render() {
        removeAll();

        shown.ifPresent(this::rows);
    }

    private void rows(final @NotNull TestCaseDto testCase) {
        addTags(testCase);

        addRow(CreateTestCaseFields.STEPS.getIcon(), Display.numberedSteps(testCase.getSteps()));

        addRow(CreateTestCaseFields.TEST_DATA.getIcon(), testCase.getTestData());
        addRow(CreateTestCaseFields.PRE_CONDITIONS.getIcon(), TestSetEditorAttributes.PRE_CONDITIONS.displayValue(testCase));

        if (cutOff) addCutOffNotice();
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217
    private void addCutOffNotice() {
        final @NotNull JBLabel notice = new JBLabel(Bundle.message("light.cut.off"));
        notice.setFont(Fonts.zoomed(Fonts.label(), zoom));
        notice.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        addRow(EmptyIcon.ICON_16, notice);
    }

    private void addRow(final @NotNull Icon icon, final @NotNull String value) {
        if (value.isBlank()) return;

        addRow(icon, prose(value));
    }

    private void addRow(final @NotNull Icon icon, final @NotNull JComponent value) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insetsBottom(GAP);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(new JBLabel(icon), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(0, GAP, GAP, 0);
        add(value, gbc);
    }

    private void addTags(final @NotNull TestCaseDto testCase) {
        final @NotNull List<Badge> badges = Badges.testCaseBadges(testCase);

        if (badges.isEmpty()) return;

        final @NotNull JBPanel<?> chips = new JBPanel<>(new HorizontalLayout(5));
        chips.setOpaque(false);
        Badges.showBadges(chips, badges);

        addRow(EmptyIcon.ICON_16, chips);
    }

    private @NotNull JTextArea prose(final @NotNull String text) {
        final @NotNull JTextArea area = Prose.of(Fonts.zoomed(Fonts.body(), zoom), JBUI.CurrentTheme.Label.foreground());
        area.setText(text);

        return area;
    }
}
