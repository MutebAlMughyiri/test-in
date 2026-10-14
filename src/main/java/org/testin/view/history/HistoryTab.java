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

package org.testin.view.history;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.git.history.BugHistory;
import org.testin.git.history.History;
import org.testin.git.history.HistoryCard;
import org.testin.git.history.TestCaseHistory;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class HistoryTab {
    private static final @NotNull String REQUEST = "testin.history.request";
    private static final int READING_AFTER_MILLIS = 300;
    private static final int GROUP = 50;
    private static final int GAP = 12;

    private static boolean isCurrent(final @NotNull JPanel historyTab, final @NotNull Object request) {
        return request.equals(historyTab.getClientProperty(REQUEST));
    }

    // Rule-VIEW-PANEL-100
    private static void show(final @NotNull Project project, final @NotNull JPanel historyTab, final @NotNull History history, final @NotNull TestCase testCase, final @NotNull Object request) {
        final @NotNull List<HistoryCard> cards = history.cards();
        if (cards.isEmpty()) {
            line(historyTab, history.problem());
            return;
        }

        final @NotNull JBPanel<?> entriesPanel = new JBPanel<>();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);
        entriesPanel.setBorder(JBUI.Borders.empty(GAP, 16, 0, 16));

        historyTab.removeAll();
        historyTab.add(entriesPanel, BorderLayout.NORTH);
        if (!history.problem().isBlank()) historyTab.add(lineLabel(history.problem()), BorderLayout.CENTER);
        draw(project, historyTab, entriesPanel, cards, testCase, 0, request);
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-101
    private static void draw(final @NotNull Project project, final @NotNull JPanel historyTab, final @NotNull JBPanel<?> entriesPanel, final @NotNull List<HistoryCard> cards, final @NotNull TestCase testCase, final int from, final @NotNull Object request) {
        if (!isCurrent(historyTab, request)) return;

        IntStream.range(from, Math.min(cards.size(), from + GROUP)).forEach(at -> entriesPanel.add(HistoryCardPanel.of(project, cards.get(at), testCase, at)));
        historyTab.revalidate();
        historyTab.repaint();

        if (from + GROUP < cards.size()) {
            ApplicationManager.getApplication().invokeLater(() -> draw(project, historyTab, entriesPanel, cards, testCase, from + GROUP, request), project.getDisposed());
        }
    }

    private static void line(final @NotNull JPanel historyTab, final @NotNull String message) {
        historyTab.removeAll();
        historyTab.add(lineLabel(message), BorderLayout.CENTER);

        historyTab.revalidate();
        historyTab.repaint();
    }

    private static @NotNull JBLabel lineLabel(final @NotNull String message) {
        final @NotNull JBLabel line = new JBLabel(message, SwingConstants.CENTER);
        line.setForeground(UIUtil.getContextHelpForeground());
        line.setBorder(JBUI.Borders.empty(20));
        return line;
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-099
    public void load(final @NotNull Project project, final @NotNull JPanel historyTab, final @NotNull Optional<TestCase> shown) {
        final @NotNull Object request = new Object();
        historyTab.putClientProperty(REQUEST, request);
        historyTab.removeAll();
        historyTab.setLayout(new BorderLayout());
        historyTab.revalidate();
        historyTab.repaint();

        if (shown.isEmpty()) return;
        final @NotNull TestCase testCase = shown.orElseThrow();

        final @NotNull Timer reading = new Timer(READING_AFTER_MILLIS, _ -> {
            if (isCurrent(historyTab, request)) line(historyTab, Bundle.message("view.history.reading"));
        });
        reading.setRepeats(false);
        reading.start();

        final @NotNull Map<Path, String> runItemsNow = BugHistory.runItemsNow(project, testCase.getUuid());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
            final @NotNull History history = testCases.testCaseFile(testCase)
                    .or(() -> TestCaseHistory.deletedFile(project, testCase.getUuid()))
                    .map(file -> BugHistory.addTo(TestCaseHistory.read(project, file, testCases.findTestCase(testCase.getUuid())), project, file, testCase.getUuid(), runItemsNow))
                    .orElseGet(() -> History.failed(Bundle.message("view.history.no.file")));

            ApplicationManager.getApplication().invokeLater(() -> {
                reading.stop();
                if (isCurrent(historyTab, request)) show(project, historyTab, history, testCase, request);
            }, project.getDisposed());
        });
    }
}
