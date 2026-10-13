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

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.git.change.FieldChange;
import org.testin.git.history.BugCard;
import org.testin.git.history.BugEvent;
import org.testin.git.history.CardKind;
import org.testin.git.history.HistoryCard;
import org.testin.git.history.TestCaseHistoryCard;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.Pill;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.CardEdge;
import org.testin.ui.framework.RowStripe;
import org.testin.ui.framework.Spacing;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;
import org.testin.view.details.AbstractDetails;
import org.testin.view.details.BugIssueLink;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class HistoryCardPanel {
    private static final int SHOWN = 2;
    private static final int CAPTION_CHARACTERS = 12;

    // Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-115
    static @NotNull JComponent of(final @NotNull Project project, final @NotNull HistoryCard card, final @NotNull TestCase testCase, final int index) {
        return switch (card) {
            case TestCaseHistoryCard entry ->
                    openDiffOnClick(project, card(index, head(card, Stream.empty(), Stream.of(muted(entry.message()))), summed(rows(entry)), Optional.empty()), entry, testCase);
            case BugCard bug ->
                    card(index, head(card, Stream.of(testRun(project, bug.event(), testCase)), Stream.empty()), bugRows(project, bug.event()), Optional.of(bug.event().kind().barOf(bug.event().runItem())));
        };
    }

    // Rule-VIEW-PANEL-116
    private static @NotNull JComponent card(final int index, final @NotNull JComponent head, final @NotNull List<JComponent> rows, final @NotNull Optional<Color> bar) {
        final @NotNull JBPanel<?> box = new JBPanel<>();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(RowStripe.of(index));
        box.setBorder(JBUI.Borders.compound(JBUI.Borders.customLineBottom(JBColor.border()), CardEdge.of(bar), JBUI.Borders.empty(8, 8, 10, 8)));

        box.add(left(head));
        rows.forEach(row -> box.add(left(row)));
        return left(box);
    }

    // Rule-VIEW-PANEL-115
    private static @NotNull JComponent head(final @NotNull HistoryCard card, final @NotNull Stream<JComponent> subject, final @NotNull Stream<JComponent> trailing) {
        final @NotNull JBPanel<?> head = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);

        Stream.of(Stream.of(pill(new Pill(card.kind().getLabel(), card.kind().getColor()))), subject, commit(card),
                        Stream.of(strong(card.who()), muted(Display.formatDate(card.when()))), trailing)
                .flatMap(part -> part)
                .filter(part -> !(part instanceof JBLabel label) || !label.getText().isBlank())
                .forEach(head::add);
        return head;
    }

    // Rule-VIEW-PANEL-097, Rule-VIEW-PANEL-098, Rule-VIEW-PANEL-115
    private static @NotNull Stream<JComponent> commit(final @NotNull HistoryCard card) {
        return Stream.of(card).filter(HistoryCard::isCommitted).map(committed -> pill(new Pill(committed.shortHash(), CardKind.PLAIN, committed.hash())));
    }

    // Rule-VIEW-PANEL-096
    private static @NotNull List<JComponent> rows(final @NotNull TestCaseHistoryCard entry) {
        return Stream.concat(
                Stream.of(entry.note()).filter(note -> !note.isBlank()).map(HistoryCardPanel::muted),
                entry.changes().stream().map(HistoryCardPanel::line)
        ).toList();
    }

    // Rule-VIEW-PANEL-116
    private static @NotNull List<JComponent> summed(final @NotNull List<JComponent> rows) {
        if (rows.size() <= SHOWN) return rows;

        final @NotNull JBPanel<?> last = new JBPanel<>(new BorderLayout(JBUI.scale(Spacing.M), 0));
        last.setOpaque(false);
        last.add(rows.get(SHOWN - 1), BorderLayout.CENTER);
        last.add(muted(Bundle.message("view.history.more", String.valueOf(rows.size() - SHOWN))), BorderLayout.EAST);

        final @NotNull List<JComponent> shown = new ArrayList<>(rows.subList(0, SHOWN));
        shown.set(SHOWN - 1, last);
        return shown;
    }

    // Rule-VIEW-PANEL-117
    private static @NotNull JComponent openDiffOnClick(final @NotNull Project project, final @NotNull JComponent card, final @NotNull TestCaseHistoryCard entry, final @NotNull TestCase testCase) {
        final @NotNull MouseAdapter open = new MouseAdapter() {
            @Override
            public void mouseClicked(final @NotNull MouseEvent click) {
                final @NotNull DiffContentFactory contents = DiffContentFactory.getInstance();
                DiffManager.getInstance().showDiff(project, new SimpleDiffRequest(testCase.getDescription(), contents.create(entry.was()), contents.create(entry.now()),
                        Bundle.message("view.history.diff.before"), Bundle.message("view.history.diff.after")));
            }
        };
        UIUtil.uiTraverser(card).forEach(part -> part.addMouseListener(open));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Tooltip.set(card, Bundle.message("view.history.compare"));
        return card;
    }

    // Rule-VIEW-PANEL-109
    private static @NotNull JComponent testRun(final @NotNull Project project, final @NotNull BugEvent event, final @NotNull TestCase testCase) {
        return Services.getInstance(project, TestRuns.class).findTestRunNode(event.testRun())
                .map(node -> {
                    final @NotNull JComponent link = AbstractDetails.link(event.testRunName(), _ -> Services.getInstance(project, TestinEditors.class).openAndSelect(node, testCase));
                    link.setFont(Fonts.strong());
                    return link;
                })
                .orElseGet(() -> {
                    final @NotNull JBLabel gone = text(event.testRunName(), Fonts.strong(), JBColor.GRAY);
                    Tooltip.set(gone, Bundle.message("view.history.bug.test.run.gone"));
                    return gone;
                });
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-108
    private static @NotNull List<JComponent> bugRows(final @NotNull Project project, final @NotNull BugEvent event) {
        return switch (event.kind()) {
            case RECORDED -> bugFields(project, event.runItem());
            case CHANGED -> event.changes().stream().map(HistoryCardPanel::line).toList();
            case CLEARED ->
                    List.of(field(Bundle.message("view.history.bug.because"), plain(Bundle.message("view.history.bug.cleared.why", event.runItem().getStatus().getLabel()))));
            case REMOVED ->
                    List.of(field(Bundle.message("view.history.bug.because"), plain(Bundle.message("view.history.bug.removed"))));
        };
    }

    private static @NotNull List<JComponent> bugFields(final @NotNull Project project, final @NotNull RunItem runItem) {
        final @NotNull List<JComponent> rows = new ArrayList<>();
        if (runItem.isFailed()) {
            rows.add(field(TestRunEditorFields.BUG_SEVERITY.getName(), text(runItem.getBugSeverity().getLabel(), Fonts.body(), runItem.getBugSeverity().getColor())));
            rows.add(field(TestRunEditorFields.BUG_PRIORITY.getName(), plain(runItem.getBugPriority().getLabel())));
        }
        runItem.bugIssue().ifPresent(url -> rows.add(field(TestRunEditorFields.BUG_ISSUE.getName(), BugIssueLink.of(project, url))));
        return rows;
    }

    // Rule-VIEW-PANEL-116
    private static @NotNull JComponent line(final @NotNull FieldChange change) {
        final @NotNull String wasAndNow = (change.oldValue() + " → " + change.newValue()).replace('\n', ' ').trim();
        final @NotNull JBLabel values = plain(wasAndNow);
        Tooltip.set(values, wasAndNow);
        return field(change.fieldName(), values);
    }

    // Rule-VIEW-PANEL-116
    private static @NotNull JComponent field(final @NotNull String name, final @NotNull JComponent value) {
        final @NotNull JBPanel<?> field = new JBPanel<>(new BorderLayout(JBUI.scale(Spacing.L), 0));
        field.setOpaque(false);
        field.setBorder(JBUI.Borders.emptyTop(Spacing.XS));
        field.add(caption(name), BorderLayout.WEST);
        field.add(value, BorderLayout.CENTER);
        return field;
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-111
    private static @NotNull JBLabel caption(final @NotNull String name) {
        final @NotNull JBLabel caption = UpdateTestCaseFields.iconOf(name).map(JBLabel::new).orElseGet(() -> named(name));
        Tooltip.set(caption, name);
        caption.getAccessibleContext().setAccessibleName(name);
        return caption;
    }

    // Rule-VIEW-PANEL-116
    private static @NotNull JBLabel named(final @NotNull String name) {
        final @NotNull JBLabel caption = muted(name);
        caption.setPreferredSize(new Dimension(caption.getFontMetrics(caption.getFont()).charWidth('m') * CAPTION_CHARACTERS, caption.getPreferredSize().height));
        return caption;
    }

    private static @NotNull JComponent pill(final @NotNull Badge badge) {
        final @NotNull JBPanel<?> holder = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        Badges.showBadges(holder, List.of(badge));
        return holder;
    }

    private static @NotNull JBLabel muted(final @NotNull String value) {
        return text(value, Fonts.body(), JBColor.GRAY);
    }

    private static @NotNull JBLabel plain(final @NotNull String value) {
        return text(value, Fonts.body(), UIUtil.getLabelForeground());
    }

    private static @NotNull JBLabel strong(final @NotNull String value) {
        return text(value, Fonts.strong(), UIUtil.getLabelForeground());
    }

    // Rule-INTERNAL-132
    private static @NotNull JBLabel text(final @NotNull String value, final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JBLabel label = new JBLabel(Display.shortDates(value));
        label.setFont(font);
        label.setForeground(color);
        Tooltip.set(label, Display.dateTooltip(value));
        return label;
    }

    private static @NotNull JComponent left(final @NotNull JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }
}
