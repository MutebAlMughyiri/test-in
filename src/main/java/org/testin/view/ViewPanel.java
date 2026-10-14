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

package org.testin.view;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinEscapeAction;
import org.testin.editor.WheelForwarding;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.testrun.RunItem;
import org.testin.runner.TestNGExecutionRecorder;
import org.testin.services.ProjectLifetime;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.ui.FollowsWidth;
import org.testin.ui.FontSync;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Stream;

public class ViewPanel implements Disposable {

    private @NotNull OptionalInt shownLine = OptionalInt.empty();

    @Getter
    private final @NotNull JBPanel<?> detailsTab;
    @Getter
    private final @NotNull JPanel historyTab;

    @Getter
    private final @NotNull JBScrollPane detailsScrollPane;
    @Getter
    private final @NotNull JBScrollPane historyScrollPane;

    @Getter
    private final @NotNull ShownTestCaseCursor page;

    @Getter
    private final @NotNull Project project;

    private final @NotNull TestCases testCases;
    private final @NotNull TestRuns testRuns;
    private final @NotNull TestinRoot testinRoot;

    public ViewPanel(final @NotNull Project project) {
        this.project = project;
        this.testCases = Services.getInstance(project, TestCases.class);
        this.testRuns = Services.getInstance(project, TestRuns.class);
        this.testinRoot = Services.getInstance(project, TestinRoot.class);
        Disposer.register(ProjectLifetime.of(project), this);
        detailsTab = new JBPanel<>(new BorderLayout());
        historyTab = new JBPanel<>(new BorderLayout());

        FontSync.syncWithNativeEditor(project, detailsTab, this);
        FontSync.syncWithNativeEditor(project, historyTab, this);

        // Rule-SETTING-039
        tabs().forEach(tab -> tab.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane));

        detailsScrollPane = createScrollPane(FollowsWidth.of(detailsTab));
        historyScrollPane = createScrollPane(historyTab);

        // Rule-VIEW-PANEL-058
        new TestinEscapeAction(project, detailsTab);
        new TestinEscapeAction(project, historyTab);

        for (final ViewTab tab : ViewTab.values()) takesTheKeyboard(tab);
        IdeEventQueue.getInstance().addPostprocessor(event -> {
            focusTabPressed(event);
            return false;
        }, this);

        page = new ShownTestCaseCursor(this);

        refreshCurrentView();

        TestNGExecutionRecorder.onReported(project, this, (testCase, testNGExecutionStatus, _, _, _) -> executionReported(testCase, testNGExecutionStatus));
    }

    private @NotNull Stream<JPanel> tabs() {
        return Stream.of(detailsTab, historyTab);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080, Rule-INTERNAL-122
    private void takesTheKeyboard(final @NotNull ViewTab shown) {
        final @NotNull JComponent tab = shown.keyboardTargetOf(this);
        tab.setFocusable(true);
        tab.setFocusTraversalKeysEnabled(false);
        tab.getAccessibleContext().setAccessibleName(shown.getDisplayName());

        new ViewTabAction(project, tab, Direction.NEXT);
        new ViewTabAction(project, tab, Direction.PREVIOUS);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-080
    private void focusTabPressed(final @NotNull AWTEvent event) {
        if (!(event instanceof MouseEvent press) || press.getID() != MouseEvent.MOUSE_PRESSED) return;

        Optional.ofNullable(SwingUtilities.getDeepestComponentAt(press.getComponent(), press.getX(), press.getY()))
                .flatMap(pressed -> tabs().filter(tab -> SwingUtilities.isDescendingFrom(pressed, tab)).findFirst())
                .ifPresent(tab -> IdeFocusManager.getInstance(project).requestFocus(tab, true));
    }

    // Rule-VIEW-PANEL-124
    private @NotNull JBScrollPane createScrollPane(final @NotNull JComponent view) {
        final @NotNull JBScrollPane scrollPane = new JBScrollPane(view);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        scrollPane.setFocusable(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-010, Rule-VIEW-PANEL-012
    public void show(final @NotNull List<TestCase> testCases, final @NotNull List<String> treePath) {
        if (testCases.isEmpty()) return;

        ViewToolWindowFactory.toolWindow(project).ifPresent(toolWindow -> toolWindow.show(() -> {
            selectDetailsTab();
            this.updateList(testCases, treePath);
        }));
    }

    // UC-VIEW-PANEL-002, Rule-VIEW-PANEL-015, Rule-VIEW-PANEL-016, Rule-VIEW-PANEL-102
    public void showIfOpen(final @NotNull List<TestCase> testCases, final @NotNull List<String> treePath) {
        if (isClosed() || testCases.isEmpty()) return;

        this.updateList(testCases, treePath);
    }

    private boolean isClosed() {
        return ViewToolWindowFactory.toolWindow(project).filter(ToolWindow::isVisible).isEmpty();
    }

    // UC-VIEW-PANEL-015
    public boolean hide() {
        if (isClosed()) return false;

        ViewToolWindowFactory.toolWindow(project).ifPresent(ToolWindow::hide);
        return true;
    }

    // UC-VIEW-PANEL-002, Rule-VIEW-PANEL-017
    public void reset() {
        this.updateList(List.of(), List.of());
    }

    private void selectDetailsTab() {
        ViewToolWindowFactory.toolWindow(project).ifPresent(toolWindow -> {
            for (final Content content : toolWindow.getContentManager().getContents()) {
                if (ViewTab.DETAILS.getDisplayName().equals(content.getDisplayName())) {
                    toolWindow.getContentManager().setSelectedContent(content);
                    break;
                }
            }
        });
    }

    // UC-VIEW-PANEL-015, Rule-VIEW-PANEL-060
    public void hide(final @NotNull List<String> closingTreePath) {
        if (isClosed()) return;
        if (!page.getTreePath().equals(closingTreePath)) return;

        this.reset();
        this.hide();
    }

    public void updateList(final @NotNull List<TestCase> testCases, final @NotNull List<String> treePath) {
        shownLine = OptionalInt.empty();
        this.page.updateList(testCases, treePath);
        this.refreshCurrentView();
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-122
    @NotNull OptionalInt shownLine() {
        return shownLine;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-122
    void showExecution(final @NotNull OptionalInt line) {
        shownLine = line;
        ViewTab.DETAILS.load(this);
    }

    // Rule-VIEW-PANEL-005, Rule-CODEGEN-111
    private void executionReported(final @NotNull TestCase testCase, final @NotNull TestNGExecutionStatus testNGExecutionStatus) {
        if (testNGExecutionStatus.stillGoing()) reloadDetailsIfShowing(testCase.getUuid());
        else if (shows(testCase.getUuid())) refreshCurrentView();
    }

    // Rule-VIEW-PANEL-005, Rule-VIEW-PANEL-125
    public void reloadDetailsIfShowing(final @NotNull UUID testCaseUuid) {
        if (shows(testCaseUuid)) ViewTab.DETAILS.load(this);
    }

    private boolean shows(final @NotNull UUID testCaseUuid) {
        return getCurrentTestCase().filter(current -> current.getUuid().equals(testCaseUuid)).isPresent();
    }

    // Rule-VIEW-PANEL-008
    public void refreshCurrentView() {
        for (final ViewTab tab : ViewTab.values()) tab.load(this);
    }

    // Rule-VIEW-PANEL-005
    public void refreshIfShowing(final @NotNull Collection<TestCase> updated) {
        getCurrentTestCase()
                .filter(current -> updated.stream().anyMatch(testCase -> testCase.getUuid().equals(current.getUuid())))
                .ifPresent(_ -> refreshCurrentView());
    }

    public @NotNull Optional<TestCase> getCurrentTestCase() {
        return page.getCurrentTestCase();
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-083
    @NotNull Optional<TestCase> shownTestCase() {
        return getCurrentTestCase().map(shown -> shownRunItem().map(RunItem::shownTestCase).orElseGet(() -> liveOf(shown)));
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-112
    @NotNull Optional<TestCase> liveTestCase() {
        return getCurrentTestCase().map(this::liveOf);
    }

    private @NotNull TestCase liveOf(final @NotNull TestCase shown) {
        return testCases.findTestCase(shown.getUuid()).orElse(shown);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-083
    @NotNull Optional<RunItem> shownRunItem() {
        final @NotNull List<String> treePath = page.getTreePath();
        if (treePath.isEmpty()) return Optional.empty();

        return getCurrentTestCase().flatMap(shown -> testRuns.findRunItems(testinRoot.resolve(treePath))
                .flatMap(testRun -> testRun.runItemOf(shown.getUuid())));
    }

    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-012
    public void focusDetailsTab() {
        selectDetailsTab();
        detailsTab.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        detailsTab.removeAll();
        historyTab.removeAll();

        ViewToolWindowFactory.onPanelDisposed(project, this);
    }
}