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
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.ui.framework.Spacing;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderSection implements TestCaseSection {
    private final @NotNull Project p;
    private final @NotNull Notifier notifier;
    private final @NotNull TestCases testCases;

    @Getter
    private final @NotNull IntegerField position;
    private final @NotNull JBLabel outOf;
    private final @NotNull JBPanel<?> wrapper;

    public OrderSection(final @NotNull Project p) {
        this.p = p;
        this.notifier = Services.getInstance(p, Notifier.class);
        this.testCases = Services.getInstance(p, TestCases.class);

        this.position = new IntegerField(Bundle.message("order.section.position"), 1, 1);
        DialogStyle.asField(this.position);
        this.position.setColumns(4);

        this.outOf = new JBLabel(Bundle.message("order.section.of", "1"));
        DialogStyle.asField(this.outOf);
        this.outOf.setBorder(JBUI.Borders.emptyLeft(Spacing.L));

        final @NotNull JBPanel<?> field = new JBPanel<>(new BorderLayout());
        field.setOpaque(false);
        field.add(this.position, BorderLayout.WEST);
        field.add(this.outOf, BorderLayout.CENTER);

        this.wrapper = createWrapper(UpdateTestCaseFields.ORDER.getIcon(), field);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto) {
        final @NotNull List<TestCaseDto> inSet = ExecutionPosition.setOf(p, dto);
        final int size = Math.max(1, inSet.size());
        final int current = Math.min(TestCaseOrder.positionOf(inSet, dto), size);

        position.setMaxValue(size);

        position.setDefaultValue(current);
        position.setValue(current);

        outOf.setText(Bundle.message("order.section.of", String.valueOf(size)));
    }

    // UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-054
    @Override
    public boolean accepts() {
        try {
            position.validateContent();
            return true;

        } catch (final ConfigurationException invalid) {
            notifier.softRefuse(p, invalid.getMessageHtml().toString());
            return false;
        }
    }

    // UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-055, Rule-EDITOR-PANEL-282
    @Override
    public @NotNull TestCaseDto applyTo(final @NotNull TestCaseDto dto) {
        final @NotNull List<TestCaseDto> inSet = ExecutionPosition.setOf(p, dto);
        final int target = position.getValue();

        if (target == TestCaseOrder.positionOf(inSet, dto)) return dto;

        final @NotNull List<TestCaseDto> others = rankTheUnranked(inSet, dto).stream()
                .filter(tc -> !tc.getId().equals(dto.getId()))
                .toList();

        final @NotNull String before = target > 1 ? others.get(target - 2).getOrder() : "";
        final @NotNull String after = target <= others.size() ? others.get(target - 1).getOrder() : "";

        return dto.edit().order(Rank.between(before, after)).build();
    }

    // UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-055
    private @NotNull List<TestCaseDto> rankTheUnranked(final @NotNull List<TestCaseDto> inSet, final @NotNull TestCaseDto dto) {
        if (inSet.stream().noneMatch(tc -> tc.getOrder().isEmpty())) return inSet;

        final @NotNull Map<UUID, TestCaseDto> placed = TestCaseOrder.place(inSet).stream()
                .collect(Collectors.toMap(TestCaseDto::getId, moved -> moved));
        final @NotNull List<TestCaseDto> ranked = placed.values().stream()
                .filter(moved -> !moved.getId().equals(dto.getId()))
                .toList();

        if (!ranked.isEmpty()) {
            TestinLogger.info("Ranking " + ranked.size() + " test case(s) that had no place, so a typed position means something");

            final @NotNull Path setPath = dto.getParent().getPath();
            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    ranked.forEach(moved -> testCases.putTestCase(setPath, moved)));
        }

        return inSet.stream().map(tc -> placed.getOrDefault(tc.getId(), tc)).toList();
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable repackAction) {
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return position;
    }

    @Override
    public void setEditable(final boolean editable) {
        position.setEnabled(editable);
    }
}
