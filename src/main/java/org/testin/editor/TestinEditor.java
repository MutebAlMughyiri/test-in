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

package org.testin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.statusbar.EditorStatusBar;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.testcase.TestCaseOrder;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestinEditor extends Disposable {
    int DEFAULT_PAGE_SIZE = 50;

    int MAX_PAGE_SIZE = 1000;

    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-222
    String PAGE_SIZE_KEY = "testin.editor.pageSize";

    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-004
    static int pageSizeOf(final @NotNull String typed) {
        final @NotNull String asked = typed.trim();

        if (asked.isEmpty() || !asked.chars().allMatch(Character::isDigit)) return DEFAULT_PAGE_SIZE;

        final long size = asked.length() > 18 ? MAX_PAGE_SIZE : Long.parseLong(asked);

        return size < 1 ? DEFAULT_PAGE_SIZE : (int) Math.min(size, MAX_PAGE_SIZE);
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-180
    default void launching(final @NotNull UUID testCaseUuid) {
    }

    @NotNull AbstractNode getParent();

    @NotNull EditorStatusBar getStatusBar();

    int getCurrentPage();

    void setCurrentPage(final int page);

    int getPageSize();

    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-107, Rule-EDITOR-PANEL-222
    void choosePageSize(final int size);

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-102
    default boolean canStepPage(final int delta) {
        final int target = getCurrentPage() + delta;

        return target >= 1 && target <= getTotalPageCount();
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-103
    default void stepPage(final int delta) {
        if (!canStepPage(delta)) return;

        setCurrentPage(getCurrentPage() + delta);
        refreshView();
    }

    // UC-EDITOR-PANEL-024
    default void refreshStatusBarSelection(final int @NotNull [] selectedIndices) {
        final int firstRow = selectedIndices.length == 0 ? 0 : selectedIndices[0];

        getStatusBar().updateSelectionState(
                selectedIndices,
                ((getCurrentPage() - 1) * getPageSize()) + firstRow,
                getShownTestCaseCount(),
                getTotalTestCaseCount(),
                sortedBy());
    }

    @NotNull String sortedBy();

    default @NotNull Optional<RunItem> runItem(final @NotNull UUID testCaseUuid) {
        return Optional.empty();
    }

    default @NotNull List<TestCase> snapshotOfAll() {
        synchronized (getAllTestCases()) {
            return new ArrayList<>(getAllTestCases());
        }
    }

    @NotNull Project getProject();

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014, Rule-INTERNAL-028
    default int positionOf(final @NotNull TestCase testCase) {
        final @NotNull List<TestCase> all = getAllTestCases();

        synchronized (all) {
            return TestCaseOrder.positionOf(all, testCase);
        }
    }

    int getTotalPageCount();

    int getShownTestCaseCount();

    int getTotalTestCaseCount();

    void refreshView();

    // Rule-EDITOR-PANEL-291
    void repaint(final @NotNull UUID testCaseUuid);

    // Rule-EDITOR-PANEL-291
    default boolean walksOn(final @NotNull UUID testCaseUuid) {
        return false;
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-117
    void reloadData();

    // UC-INTERNAL-002, Rule-INTERNAL-114
    void followTheIndex();

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    default boolean isBusy() {
        return false;
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-119
    default boolean isLoading() {
        return false;
    }

    @NotNull List<TestCase> getSelectedTestCases();

    // UC-EDITOR-PANEL-005
    default void appendNewTestCase(final @NotNull TestCase testCase, final @NotNull Runnable onPersisted) {
        TestinLogger.debug("This editor does not create test cases; '" + testCase.getDescription() + "' was not added");
    }

    @NotNull JComponent getComponent();

    @NotNull JComponent getPreferredFocusedComponent();

    // UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-013
    default void refreshOrdered() {
        refreshView();
    }

    default void reorderAndPersist() {
    }

    void onToolBarFilterSelectionChanged();

    // UC-REPORT-001
    default @NotNull Optional<TestRunNode> shownTestRun() {
        return Optional.empty();
    }

    @NotNull List<TestCase> getAllTestCases();

    void selectTestCase(final @NotNull TestCase testCase);

    void selectWhenLoaded(final @NotNull UUID testCaseUuid);

    default void runWhenLoaded() {
    }

    @Override
    default void dispose() {
        ViewToolWindowFactory.panel(getProject()).ifPresent(viewer -> viewer.hide(getParent().treePath()));
    }
}