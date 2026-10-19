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
import org.testin.model.TestCase;
import org.testin.services.ProjectLifetime;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

// UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006
@Getter
public abstract class AbstractTestCaseDialog extends AbstractFrameworkDialog {
    @Getter(AccessLevel.NONE)
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

    protected final @NotNull TestCaseStatusSection testCaseStatusSection;

    protected final @NotNull ReferenceSection referenceSection;

    protected final @NotNull Disposable dialogDisposable;

    @Getter(AccessLevel.NONE)
    private final @NotNull TestCase testCase;

    @Getter(AccessLevel.NONE)
    private final @NotNull Consumer<@NotNull TestCase> onSave;

    @Getter(AccessLevel.NONE)
    private final StatusBarItem @NotNull [] dialogKeys;

    private final @NotNull List<TestCaseSection> cachedSections;

    private @NotNull PropertyChangeListener focusListener = NOTHING_ON_FOCUS;

    private @NotNull Optional<TestCaseSection> editableSection = Optional.empty();

    public AbstractTestCaseDialog(final @NotNull Project project, final @NotNull TestCase testCase, final @NotNull Consumer<@NotNull TestCase> onSave, final StatusBarItem @NotNull ... dialogKeys) {
        super(project);
        this.testCase = testCase;
        this.onSave = onSave;
        this.dialogKeys = dialogKeys.clone();
        this.dialogDisposable = Disposer.newDisposable("testin.testCaseDialog");
        Disposer.register(ProjectLifetime.of(project), dialogDisposable);

        this.descriptionSection = new DescriptionSection(project);
        this.expectedResultSection = new ExpectedResultSection(project);
        this.moduleSection = new ModuleSection(project);
        this.testDataSection = new TestDataSection(project);
        this.preConditionsSection = new PreConditionsSection(project);
        this.stepsSection = new StepsSection(project);
        this.prioritySection = new PrioritySection();
        this.groupSection = new GroupSection(project);
        this.orderSection = new OrderSection(project);
        this.testCaseStatusSection = new TestCaseStatusSection();
        this.referenceSection = new ReferenceSection(project);

        this.cachedSections = Stream.concat(
                        Arrays.stream(CreateTestCaseFields.values()).map(CreateTestCaseFields::getSectionExtractor),
                        Arrays.stream(UpdateTestCaseFields.values()).map(UpdateTestCaseFields::getSectionExtractor))
                .map(extractor -> extractor.apply(this))
                .distinct()
                .toList();
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-218
    private static boolean popupClaims(final @NotNull CustomShortcutSet shortcutSet) {
        return Arrays.stream(shortcutSet.getShortcuts())
                .filter(KeyboardShortcut.class::isInstance)
                .map(KeyboardShortcut.class::cast)
                .map(KeyboardShortcut::getFirstKeyStroke)
                .anyMatch(stroke -> stroke.getModifiers() == 0);
    }

    private @NotNull Optional<TestCaseSection> sectionHolding(final @NotNull Component focusOwner) {
        return getAllSections().stream()
                .filter(section -> UIUtil.isDescendingFrom(focusOwner, section.getWrapper()))
                .findFirst();
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-199
    protected abstract StatusBarItem @NotNull [] keysOf(final @NotNull TestCaseSection section);

    protected void installDynamicStatusBar(final @NotNull JComponent parentPanel) {
        focusListener = e -> Optional.ofNullable((Component) e.getNewValue())
                .filter(focusOwner -> UIUtil.isDescendingFrom(focusOwner, parentPanel))
                .flatMap(this::sectionHolding)
                .ifPresent(section -> showSectionKeys(keysOf(section)));
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener);

        Disposer.register(dialogDisposable, this::removeFocusListener);
    }

    private void removeFocusListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
        focusListener = NOTHING_ON_FOCUS;
    }

    // Rule-EDITOR-PANEL-199, Rule-EDITOR-PANEL-200
    protected final void showSectionKeys(final StatusBarItem @NotNull [] items) {
        final @NotNull List<StatusBarItem> all = new ArrayList<>(List.of(items));
        all.addAll(List.of(dialogKeys));

        showKeys(all.toArray(StatusBarItem[]::new));
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-293
    protected final void registerDialogKeys(final @NotNull JComponent keys) {
        getAllSections().forEach(section -> section.enableMultiLine(this, this::submit));
        registerShortcut(keys, Shortcuts.Enter.getCustomShortcut(), this::submit);
        registerShortcut(keys, Shortcuts.Escape.getCustomShortcut(), this::closeCancel);
        registerShortcut(keys, Shortcuts.AddItem.getCustomShortcut(), this::addBoxWhereTheCursorIs);
    }

    // Rule-EDITOR-PANEL-293
    private void addBoxWhereTheCursorIs() {
        Optional.ofNullable(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()).ifPresent(this::addBoxAt);
    }

    // Rule-EDITOR-PANEL-293
    void addBoxAt(final @NotNull Component focusOwner) {
        sectionHolding(focusOwner).filter(this::mayWrite).ifPresent(section -> section.addBox(this::refit));
    }

    @Override
    protected void closed() {
        Disposer.dispose(dialogDisposable);
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-035
    protected void onlyEditable(final @NotNull TestCaseSection target) {
        editableSection = Optional.of(target);
        getAllSections().forEach(section -> section.setEditable(section.equals(target)));
    }

    private boolean mayWrite(final @NotNull TestCaseSection section) {
        return section.isShown() && editableSection.map(target -> target.equals(section)).orElse(true);
    }

    public @NotNull List<TestCaseSection> getAllSections() {
        return cachedSections;
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-218
    @Override
    protected boolean claimedElsewhere(final @NotNull CustomShortcutSet shortcutSet) {
        return aPopupIsOpen() && popupClaims(shortcutSet);
    }

    private boolean completionIsOpen() {
        return LookupManager.getInstance(project).getActiveLookup() != null;
    }

    private boolean aPopupIsOpen() {
        return completionIsOpen();
    }

    // Rule-EDITOR-PANEL-029, Rule-EDITOR-PANEL-035
    @Override
    protected void submit() {
        final @NotNull List<TestCaseSection> writers = getAllSections().stream().filter(this::mayWrite).toList();

        if (!writers.stream().allMatch(TestCaseSection::accepts)) return;

        if (writers.contains(descriptionSection) && descriptionSection.typed().trim().isEmpty()) {
            descriptionSection.setError(true);
            return;
        }

        TestCase edited = testCase;
        for (final TestCaseSection section : writers) edited = section.applyTo(edited);
        onSave.accept(edited);

        closeOk();
    }
}