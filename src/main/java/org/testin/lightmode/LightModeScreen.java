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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.Animator;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.actions.ActionSystem;
import org.testin.actions.Declared;
import org.testin.codegen.AutomationState;
import org.testin.editor.testrun.ExecutionControl;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.StartExecutionButton;
import org.testin.model.Automated;
import org.testin.model.StatusBarItem;
import org.testin.model.TestCase;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testin.quickaction.HoverButton;
import org.testin.quickaction.ShownTestCaseAction;
import org.testin.quickaction.TestCaseQuickAction;
import org.testin.services.Services;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.RunItemRecorder;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.Prose;
import org.testin.ui.framework.ShortcutStrip;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class LightModeScreen {
    private static final int START_WIDTH = 420;

    private static final int SET_FRAME_ARC = 20;

    private static final int BUTTON_GAP = 8;

    private static final @NotNull List<TestCaseQuickAction> KEYED = List.of(TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD, TestCaseQuickAction.RUN_TEST_METHOD);

    private final @NotNull TestRunEditor editor;

    private final @NotNull RunItemRecorder runItemRecorder;

    private final @NotNull AutomationState automationState;

    private final @NotNull Runnable onClosed;

    private final @NotNull TitleBarButton start = new TitleBarButton(ExecutionControl.START.getLabel(), ExecutionControl.START.getIcon());

    private final @NotNull TitleBarButton stop = new TitleBarButton(ExecutionControl.STOP.getLabel(), ExecutionControl.STOP.getIcon());

    private final @NotNull JBLabel counter = new JBLabel();

    private final @NotNull Font setFont = Fonts.label();

    private final @NotNull Font descriptionFont = Fonts.title();

    private final @NotNull Font expectedFont = Fonts.body();

    private final @NotNull JBLabel set = new JBLabel();

    private final @NotNull JTextArea description = Prose.of(descriptionFont, JBUI.CurrentTheme.Label.foreground());

    private final @NotNull JTextArea expected = Prose.of(expectedFont, JBUI.CurrentTheme.ContextHelp.FOREGROUND);

    private final @NotNull JComponent expectedRow = iconBefore(CreateTestCaseFields.EXPECTED_RESULT.getIcon(), expected);

    private final @NotNull JBLabel idle = new JBLabel(Bundle.message("light.idle"), SwingConstants.CENTER);

    private final @NotNull JBLabel chosen = new JBLabel();

    private final @NotNull SlidingPanel testCaseView = new SlidingPanel(new BorderLayout());

    private final @NotNull Disposable motionScope = Disposer.newDisposable("Testin light mode motion");

    private final @NotNull LightModeFrame frame = new LightModeFrame(motionScope);

    private final @NotNull TestCaseDetails details;

    private final @NotNull JBPanel<?> underTestCase = new JBPanel<>(new BorderLayout());

    private final @NotNull JBLabel testCaseClock = clock(Bundle.message("light.test.case.clock"));

    private final @NotNull JBLabel testRunClock = clock(Bundle.message("light.test.run.clock"));

    private final @NotNull JBPanel<?> strip = new JBPanel<>(new BorderLayout());

    private final @NotNull JBPanel<?> setLine = new JBPanel<>(new GridBagLayout());

    private final @NotNull JBPanel<?> buttons = new JBPanel<>(new GridLayout(1, 0, JBUI.scale(BUTTON_GAP), 0));

    private final @NotNull JBPanel<?> footer = new JBPanel<>(new BorderLayout());

    private final @NotNull JComponent runItemStatusRow = runItemStatusButtons();

    private final @NotNull ShortcutStrip statusBar = new ShortcutStrip(new StatusBarItem[0]);

    private final @NotNull ViewMenuButton viewMenu = new ViewMenuButton(this::applyView);

    private final @NotNull LightModeZoom zoom = LightModeZoom.remembered();

    private @NotNull Optional<UUID> shownTestCase = Optional.empty();

    private @NotNull Optional<Animator> slideMotion = Optional.empty();

    private @NotNull Optional<FailureForm> capture = Optional.empty();

    private boolean detailsKeyHeld = false;

    // Rule-EDITOR-PANEL-128
    LightModeScreen(final @NotNull TestRunEditor editor, final @NotNull Runnable onClosed) {
        this.editor = editor;
        this.runItemRecorder = Services.getInstance(editor.getProject(), RunItemRecorder.class);
        this.automationState = Services.getInstance(editor.getProject(), AutomationState.class);
        this.details = new TestCaseDetails();
        this.onClosed = onClosed;

        frame.hold(content());

        applyZoom();

        frame.packAndPlace();
        refresh();

        bindKeys();
        bindWheel();
        frame.resizable(this::fitHeight);

        frame.show();
    }

    private static @NotNull String keyOf(final @NotNull RunItemStatus runItemStatus) {
        return Shortcuts.shortcutText(runItemStatus.getShortcut());
    }

    private static @NotNull JComponent iconBefore(final @NotNull Icon icon, final @NotNull JComponent text) {
        return JBUI.Panels.simplePanel(TestCaseDetails.GAP, 0).addToLeft(new JBLabel(icon)).addToCenter(text).andTransparent();
    }

    private static @NotNull JBLabel clock(final @NotNull String meaning) {
        final @NotNull JBLabel label = new JBLabel();
        Tooltip.set(label, meaning);
        label.setFont(Fonts.small());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        return label;
    }

    boolean shows(final @NotNull TestRunNode other) {
        return editor.getParent().getPath().equals(other.getPath());
    }

    // Rule-EDITOR-PANEL-283
    void refresh() {
        final @NotNull List<TestCase> testCases = editor.getCurrentTestCases();
        final int index = editor.getWalk().getCurrentlyExecutingIndex();
        final boolean executing = index >= 0 && index < testCases.size();

        counter.setText(executing
                ? Bundle.message("light.counter.position", String.valueOf(index + 1), String.valueOf(testCases.size()))
                : Bundle.message("light.counter.test.cases", String.valueOf(testCases.size())));

        idle.setVisible(!executing);
        testCaseView.setVisible(executing);
        footer.setVisible(executing);

        // Rule-EDITOR-PANEL-290
        start.setVisible(editor.getWalk().mode().showsStart());
        stop.setVisible(editor.getWalk().mode().showsStop());

        // Rule-EDITOR-PANEL-135
        start.setEnabled(editor.getWalk().canStartManualExecution());
        Tooltip.set(start, StartExecutionButton.tooltipFor(editor));

        final @NotNull Optional<UUID> wasShowing = shownTestCase;
        if (executing) showTestCase(testCases.get(index));

        if (!executing || !shownTestCase.equals(wasShowing)) capture = Optional.empty();

        if (capture.isPresent()) applyParts();
        else showCapture();

        tick();

        fitHeight();
        frame.repaint();
    }

    void tick() {
        testCaseClock.setText(Display.formatTestCaseClock(editor.getWalk().getCurrentTestCaseElapsed()));
        testRunClock.setText(Display.formatTestRunClock(editor.getElapsed()));
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-202, Rule-EDITOR-PANEL-204
    private void fitHeight() {
        frame.fitHeight(details::setCutOff);
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-201, Rule-EDITOR-PANEL-294
    private void showTestCase(final @NotNull TestCase testCase) {
        final boolean arrived = shownTestCase.map(previous -> !previous.equals(testCase.getUuid())).orElse(false);
        shownTestCase = Optional.of(testCase.getUuid());

        if (arrived) testCaseView.captureLeaving();

        set.setText(testCase.getParent().getName());
        description.setText(TestSetEditorFields.DESCRIPTION.displayValue(testCase));
        description.setForeground(testCase.getStatus().getDescriptionColor());
        expected.setText(TestSetEditorFields.EXPECTED_RESULT.displayValue(testCase));

        details.show(testCase);

        if (arrived) slideTestCaseIn();
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-201, Rule-EDITOR-PANEL-204
    private void slideTestCaseIn() {
        if (!testCaseView.hasSomethingToSlide()) return;

        slideMotion.ifPresent(Animator::dispose);

        slideMotion = Motion.run(motionScope, "Testin light mode test case",
                testCaseView::setTravelled,
                () -> testCaseView.setTravelled(1.0));
    }

    @TestOnly
    @NotNull LightModeFrame frame() {
        return frame;
    }

    @TestOnly
    @NotNull Optional<Animator> slideMotion() {
        return slideMotion;
    }

    void close() {
        closeQuietly();

        onClosed.run();
    }

    void closeQuietly() {
        Disposer.dispose(motionScope);
        frame.closeQuietly();
    }

    private void bindKeys() {
        frame.bind(Shortcuts.Escape.getKey(), "testin.lightMode.escape", this::escape);
        frame.bind(Shortcuts.Enter.getKey(), "testin.lightMode.commit", this::saveCapture);
        final @NotNull KeyStroke details = Shortcuts.ToggleDetails.getKey();
        frame.bind(details, "testin.lightMode.toggleDetails", this::toggleDetailsOnce);
        frame.bind(KeyStroke.getKeyStroke(details.getKeyCode(), details.getModifiers(), true), "testin.lightMode.releaseDetails", () -> detailsKeyHeld = false);
        frame.bind(KeyStroke.getKeyStroke(details.getKeyCode(), 0, true), "testin.lightMode.releaseDetailsAlone", () -> detailsKeyHeld = false);

        for (final RunItemStatus runItemStatus : RunItemStatus.values()) {
            if (!runItemStatus.isJudged()) continue;

            frame.bind(runItemStatus.getShortcut(), "testin.lightMode." + runItemStatus.name(), () -> judge(runItemStatus));
        }

        KEYED.forEach(button -> ShownTestCaseAction.bind(editor.getProject(), button, this::testCaseForButtons, (action, testCase) -> action.executeFor(editor, testCase), frame.rootPane()));

        frame.onClosing(this::close);
    }

    private void bindWheel() {
        frame.content().addMouseWheelListener(e -> {
            zoomBy(-e.getWheelRotation() * LightModeZoom.STEP);
            e.consume();
        });
    }

    // UC-EDITOR-PANEL-046
    private void zoomBy(final float delta) {
        if (!zoom.by(delta)) return;

        zoom.remember();

        applyZoom();
        fitHeight();
    }

    private void applyZoom() {
        set.setFont(zoom.scaled(setFont));

        chosen.setFont(zoom.scaled(setFont));
        description.setFont(zoom.scaled(descriptionFont));
        expected.setFont(zoom.scaled(expectedFont));

        details.setZoom(zoom.getLevel());
        capture.ifPresent(form -> form.setZoom(zoom.getLevel()));
    }

    // UC-EDITOR-PANEL-046
    private void toggleDetailsOnce() {
        if (detailsKeyHeld) return;

        detailsKeyHeld = true;
        toggleDetails();
    }

    private void toggleDetails() {
        if (capture.isPresent()) return;

        details.setVisible(!details.isVisible());

        fitHeight();
    }

    // UC-EDITOR-PANEL-046
    private void judge(final @NotNull RunItemStatus runItemStatus) {
        if (capture.isPresent()) return;

        if (runItemStatus.collectsFailureDetails()) {
            openCapture();
            return;
        }

        record(runItemStatus);
    }

    private void record(final @NotNull RunItemStatus runItemStatus) {
        runItemRecorder.executeNext(editor, runItemStatus);
    }

    private void openCapture() {
        executingRunItem().ifPresent(runItem -> {
            capture = Optional.of(new FailureForm(editor.getProject(), editor.getParent().getPath(), runItem, zoom.getLevel(), this::fitHeight, this::saveCapture));

            showCapture();
            fitHeight();

            capture.ifPresent(FailureForm::focusFirstField);
        });
    }

    private void cancelCapture() {
        capture = Optional.empty();

        showCapture();
        fitHeight();
    }

    private void saveCapture() {
        capture.ifPresent(form -> {
            // Rule-EDITOR-PANEL-225, Rule-EDITOR-PANEL-285
            if (!runItemRecorder.executeFailed(editor, form.fields())) return;

            capture = Optional.empty();
            refresh();
        });
    }

    // UC-EDITOR-PANEL-046
    private void escape() {
        if (capture.isPresent()) {
            cancelCapture();
            return;
        }

        close();
    }

    // UC-EDITOR-PANEL-046
    private void showCapture() {
        underTestCase.removeAll();

        ActionSystem.perform(frame.rootPane(), ActionPlaces.UNKNOWN, ActionUiKind.NONE, () -> underTestCase.add(capture.map(form -> (JComponent) form).orElse(details), BorderLayout.CENTER));

        statusBar.updateItems(capture.isPresent() ? commitKeys() : testCaseKeys());

        applyParts();
    }

    private void applyView() {
        applyParts();
        fitHeight();
    }

    private void applyParts() {
        final boolean writing = capture.isPresent();

        set.setVisible(shows(LightModePart.SET_NAME));
        chosen.setVisible(writing);
        chosen.setText(set.isVisible() ? " · " + RunItemStatus.FAILED.getLabel() : RunItemStatus.FAILED.getLabel());
        showButtons();
        setLine.setVisible(set.isVisible() || chosen.isVisible() || buttons.isVisible());

        expectedRow.setVisible(!expected.getText().isBlank());

        strip.setVisible(shows(LightModePart.DURATION));
        runItemStatusRow.setVisible(shows(LightModePart.RUN_ITEM_STATUS_BUTTONS) && !writing);
        statusBar.setShown(shows(LightModePart.STATUS_BAR) || writing);
    }

    private boolean shows(final @NotNull LightModePart part) {
        return viewMenu.getSelectedDetails().contains(part);
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-243, Rule-EDITOR-PANEL-244
    private void showButtons() {
        buttons.removeAll();

        testCaseForButtons().ifPresent(testCase -> {
            final @NotNull Project project = editor.getProject();
            final @NotNull Automated automation = automationState.of(testCase.getUuid());

            TestCaseQuickAction.onCard(project, editor.getParent(), testCase).stream()
                    .filter(offered -> viewMenu.getSelectedDetails().stream().anyMatch(part -> part.governs(offered.action())))
                    .forEach(offered -> buttons.add(HoverButton.of(project, offered, offered.action().iconOn(automation), offered.action().getTooltip(), () -> offered.action().executeFor(editor, testCase))));
        });

        buttons.setVisible(buttons.getComponentCount() > 0);
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-245
    private @NotNull Optional<TestCase> testCaseForButtons() {
        return capture.isPresent() ? Optional.empty() : executingTestCase();
    }

    private @NotNull Optional<TestCase> executingTestCase() {
        final @NotNull List<TestCase> testCases = editor.getCurrentTestCases();
        final int index = editor.getWalk().getCurrentlyExecutingIndex();

        return index >= 0 && index < testCases.size() ? Optional.of(testCases.get(index)) : Optional.empty();
    }

    private @NotNull Optional<RunItem> executingRunItem() {
        return executingTestCase()
                .flatMap(testCase -> editor.runItem(testCase.getUuid()))
                .filter(runItem -> !runItem.isRemoved());
    }

    private @NotNull JComponent content() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        panel.setBackground(JBUI.CurrentTheme.CustomFrameDecorations.paneBackground());
        panel.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                JBUI.Borders.empty(0, LightModeFrame.GRAB)));

        panel.add(titleBar(), BorderLayout.NORTH);
        panel.add(body(), BorderLayout.CENTER);
        panel.add(footer(), BorderLayout.SOUTH);

        return panel;
    }

    private @NotNull JComponent titleBar() {
        final @NotNull JBPanel<?> bar = new JBPanel<>(new BorderLayout(JBUI.scale(6), 0));
        bar.setBorder(JBUI.Borders.empty(4, 6));

        bar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        start.addActionListener(_ -> editor.onStartExecutionClicked());
        stop.addActionListener(_ -> editor.onStopExecutionClicked());

        final @NotNull JBPanel<?> left = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(2), 0));
        left.setOpaque(false);
        left.add(start);
        left.add(stop);
        left.add(frame.pin());
        left.add(viewMenu);

        counter.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        bar.add(left, BorderLayout.WEST);
        final @NotNull JBLabel testRunName = new JBLabel(editor.getParent().getName());
        testRunName.setFont(Fonts.body());

        bar.add(testRunName, BorderLayout.CENTER);
        bar.add(counter, BorderLayout.EAST);

        frame.dragBy(bar);

        return bar;
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-232, Rule-EDITOR-PANEL-243
    private @NotNull JComponent body() {
        set.setForeground(Icons.GRAY);
        set.setBorder(JBUI.Borders.compound(new RoundedLineBorder(Icons.GRAY, JBUI.scale(SET_FRAME_ARC), 1), JBUI.Borders.empty(1, 7)));
        idle.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        chosen.setForeground(RunItemStatus.FAILED.getRowColor());

        final @NotNull JBPanel<?> text = new JBPanel<>(new BorderLayout(0, JBUI.scale(10)));
        text.setOpaque(false);
        text.add(iconBefore(CreateTestCaseFields.DESCRIPTION.getIcon(), description), BorderLayout.NORTH);
        text.add(expectedRow, BorderLayout.CENTER);

        details.setBorder(JBUI.Borders.emptyTop(14));
        details.setVisible(false);

        buttons.setOpaque(false);

        final @NotNull GridBagConstraints onOneRow = new GridBagConstraints();
        onOneRow.gridy = 0;

        setLine.setOpaque(false);
        setLine.setBorder(JBUI.Borders.emptyBottom(TestCaseDetails.GAP));
        setLine.add(set, onOneRow);
        setLine.add(chosen, onOneRow);
        onOneRow.weightx = 1;
        setLine.add(Box.createHorizontalGlue(), onOneRow);
        onOneRow.weightx = 0;
        setLine.add(buttons, onOneRow);

        underTestCase.setOpaque(false);

        testCaseView.setOpaque(false);
        testCaseView.add(setLine, BorderLayout.NORTH);
        testCaseView.add(text, BorderLayout.CENTER);
        testCaseView.add(underTestCase, BorderLayout.SOUTH);

        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout()) {
            @Override
            public @NotNull Dimension getPreferredSize() {
                return new Dimension(JBUI.scale(START_WIDTH), super.getPreferredSize().height);
            }
        };

        panel.setBorder(JBUI.Borders.empty(14));
        panel.add(idle, BorderLayout.CENTER);
        panel.add(testCaseView, BorderLayout.NORTH);

        return panel;
    }

    private @NotNull JComponent runItemStatusButtons() {
        final @NotNull JBPanel<?> runItemStatuses = new JBPanel<>(new GridLayout(1, 0, JBUI.scale(6), 0));
        runItemStatuses.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
        runItemStatuses.setOpaque(false);

        for (final RunItemStatus runItemStatus : RunItemStatus.values()) {
            if (!runItemStatus.isJudged()) continue;

            runItemStatuses.add(new RunItemStatusButton(keyOf(runItemStatus), runItemStatus.getLabel(), () -> judge(runItemStatus)));
        }

        return runItemStatuses;
    }

    private @NotNull JComponent footer() {
        footer.setOpaque(false);
        footer.add(runItemStatusRow, BorderLayout.NORTH);
        footer.add(durationStrip(), BorderLayout.CENTER);
        footer.add(statusBar.getPanel(), BorderLayout.SOUTH);

        return footer;
    }

    private @NotNull JComponent durationStrip() {
        strip.setBorder(JBUI.Borders.empty(0, 10, 8, 10));
        strip.setOpaque(false);
        strip.add(testCaseClock, BorderLayout.WEST);
        strip.add(testRunClock, BorderLayout.EAST);

        return strip;
    }

    private StatusBarItem @NotNull [] testCaseKeys() {
        final @NotNull List<StatusBarItem> statusBarItems = new ArrayList<>();
        statusBarItems.add(StatusBarShortcut.hint(Shortcuts.ToggleDetails.getShortcutText(), Bundle.message("shortcut.details")));
        statusBarItems.add(StatusBarShortcut.hint(Shortcuts.Escape.getShortcutText(), Bundle.message("shortcut.close")));

        for (final RunItemStatus runItemStatus : RunItemStatus.values()) {
            if (runItemStatus.isJudged())
                statusBarItems.add(StatusBarShortcut.hint(keyOf(runItemStatus), runItemStatus.getLabel()));
        }

        KEYED.forEach(button -> statusBarItems.add(keyHint(button)));

        return statusBarItems.toArray(new StatusBarItem[0]);
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-245
    private @NotNull StatusBarItem keyHint(final @NotNull TestCaseQuickAction button) {
        final @NotNull TestCaseQuickAction now = testCaseForButtons()
                .map(testCase -> button.gestureOn(editor.getProject(), testCase))
                .orElse(button);

        return StatusBarShortcut.hint(Declared.shortcutText(button.getActionId()), now.getTooltip());
    }

    private StatusBarItem @NotNull [] commitKeys() {
        return new StatusBarItem[]{
                StatusBarShortcut.hint(Shortcuts.Enter.getShortcutText(), Bundle.message("shortcut.save.and.next")),
                StatusBarShortcut.hint(Shortcuts.Escape.getShortcutText(), Bundle.message("shortcut.cancel")),
                StatusBarShortcut.corrections(),
                StatusBarShortcut.pasteScreenshot()};
    }
}
