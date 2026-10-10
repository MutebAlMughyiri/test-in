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

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.model.TestCaseDto;
import org.testin.services.ProjectLifetime;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.AbstractFrameworkDialog;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

// UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006
@Getter
public abstract class TestCaseBaseDialog extends AbstractFrameworkDialog {
    @Getter(AccessLevel.NONE)
    private static final @NotNull StatusBarItem[] NO_ITEMS = new StatusBarItem[0];
    private static final @NotNull PropertyChangeListener NOTHING_ON_FOCUS = _ -> {
    };
    protected final @NotNull DescriptionSection descriptionSection;
    protected final @NotNull ExpectedResultSection expectedResultSection;
    protected final @NotNull ModuleSection moduleSection;
    protected final @NotNull TestDataSection testDataSection;
    protected final @NotNull PreConditionsSection preConditionsSection;
    protected final @NotNull PrioritySection prioritySection;
    protected final @NotNull GroupSection groupSection;
    protected final @NotNull StepsSection stepsSection;
    protected final @NotNull OrderSection orderSection;
    protected final @NotNull StatusSection statusSection;
    protected final @NotNull ReferenceSection referenceSection;
    protected final @NotNull Disposable dialogDisposable;
    protected final @NotNull Map<CreateTestCaseSection, StatusBarItem[]> statusBarMapping;
    @Getter(AccessLevel.NONE)
    private final @NotNull TestCaseDto dto;
    @Getter(AccessLevel.NONE)
    private final @NotNull Consumer<@NotNull TestCaseDto> onSave;
    @Getter(AccessLevel.NONE)
    private final StatusBarItem @NotNull [] dialogKeys;
    private final @NotNull List<CreateTestCaseSection> cachedSections;
    private @NotNull PropertyChangeListener focusListener = NOTHING_ON_FOCUS;
    private @NotNull Optional<CreateTestCaseSection> editableSection = Optional.empty();

    public TestCaseBaseDialog(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull Consumer<@NotNull TestCaseDto> onSave, final StatusBarItem @NotNull ... dialogKeys) {
        super(p);
        this.dto = dto;
        this.onSave = onSave;
        this.dialogKeys = dialogKeys.clone();
        this.dialogDisposable = Disposer.newDisposable("testin.testCaseDialog");
        Disposer.register(ProjectLifetime.of(p), dialogDisposable);

        this.descriptionSection = new DescriptionSection(p);
        this.expectedResultSection = new ExpectedResultSection(p);
        this.moduleSection = new ModuleSection(p);
        this.testDataSection = new TestDataSection(p);
        this.preConditionsSection = new PreConditionsSection(p);
        this.stepsSection = new StepsSection(p);
        this.prioritySection = new PrioritySection();
        this.groupSection = new GroupSection(p);
        this.orderSection = new OrderSection(p);
        this.statusSection = new StatusSection();
        this.referenceSection = new ReferenceSection(p);

        this.cachedSections = Stream.concat(
                        Arrays.stream(CreateTestCaseFields.values()).map(CreateTestCaseFields::getSectionExtractor),
                        Arrays.stream(UpdateTestCaseFields.values()).map(UpdateTestCaseFields::getSectionExtractor))
                .map(extractor -> extractor.apply(this))
                .distinct()
                .toList();

        final @NotNull Map<CreateTestCaseSection, StatusBarItem[]> bars = new LinkedHashMap<>();
        for (final CreateTestCaseFields field : CreateTestCaseFields.values())
            bars.put(field.getSectionExtractor().apply(this), field.getStatusBarItems());

        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values())
            bars.putIfAbsent(field.getSectionExtractor().apply(this), field.getStatusBarItems());

        this.statusBarMapping = Map.copyOf(bars);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-218
    private static boolean popupClaims(final @NotNull CustomShortcutSet shortcutSet) {
        return Arrays.stream(shortcutSet.getShortcuts())
                .filter(KeyboardShortcut.class::isInstance)
                .map(KeyboardShortcut.class::cast)
                .map(KeyboardShortcut::getFirstKeyStroke)
                .anyMatch(stroke -> stroke.getModifiers() == 0);
    }

    private @NotNull Optional<CreateTestCaseSection> sectionHolding(final @NotNull Component focusOwner) {
        return getAllSections().stream()
                .filter(section -> UIUtil.isDescendingFrom(focusOwner, section.getWrapper()))
                .findFirst();
    }

    protected void initDynamicStatusBar(final @NotNull JComponent parentPanel) {
        focusListener = evt -> Optional.ofNullable((Component) evt.getNewValue())
                .filter(focusOwner -> UIUtil.isDescendingFrom(focusOwner, parentPanel))
                .flatMap(this::sectionHolding)
                .ifPresent(section -> showSectionKeys(statusBarMapping.getOrDefault(section, NO_ITEMS)));
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener);

        Disposer.register(dialogDisposable, this::removeFocusListener);
    }

    private void removeFocusListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
        focusListener = NOTHING_ON_FOCUS;
    }

    // Rule-EDITOR-PANEL-199
    protected final void showSectionKeys(final StatusBarItem @NotNull [] items) {
        final @NotNull List<StatusBarItem> all = new ArrayList<>(List.of(items));
        all.addAll(List.of(dialogKeys));

        showKeys(all.toArray(StatusBarItem[]::new));
    }

    @Override
    protected void closed() {
        Disposer.dispose(dialogDisposable);
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-035
    protected void onlyEditable(final @NotNull CreateTestCaseSection target) {
        editableSection = Optional.of(target);
        getAllSections().forEach(section -> section.setEditable(section.equals(target)));
    }

    private boolean mayWrite(final @NotNull CreateTestCaseSection section) {
        return section.isShown() && editableSection.map(target -> target.equals(section)).orElse(true);
    }

    public @NotNull List<CreateTestCaseSection> getAllSections() {
        return cachedSections;
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-218
    @Override
    protected boolean claimedElsewhere(final @NotNull CustomShortcutSet shortcutSet) {
        return aPopupIsOpen() && popupClaims(shortcutSet);
    }

    private boolean completionIsOpen() {
        return LookupManager.getInstance(p).getActiveLookup() != null;
    }

    private boolean aPopupIsOpen() {
        return completionIsOpen();
    }

    // Rule-EDITOR-PANEL-029, Rule-EDITOR-PANEL-035
    @Override
    protected void submit() {
        final @NotNull List<CreateTestCaseSection> writers = getAllSections().stream().filter(this::mayWrite).toList();

        if (!writers.stream().allMatch(CreateTestCaseSection::accepts)) return;

        if (writers.contains(descriptionSection) && descriptionSection.typed().trim().isEmpty()) {
            descriptionSection.setError(true);
            return;
        }

        TestCaseDto edited = dto;
        for (final CreateTestCaseSection section : writers) edited = section.applyTo(edited);
        onSave.accept(edited);

        closeOk();
    }
}