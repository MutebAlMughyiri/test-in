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

package org.testin.git.review;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.git.change.ChangeSubject;
import org.testin.git.change.ChangeType;
import org.testin.git.change.FieldChange;
import org.testin.git.change.PendingChange;
import org.testin.git.change.TestCaseFieldChanges;
import org.testin.indexer.TestCases;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.TestCase.TestCaseBuilder;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ChoiceInput;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.DialogSplitButton;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Shortcuts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class PendingChangesDialog extends AbstractFrameworkDialog {
    private static final @NotNull String PUSH = Bundle.message("dialog.pending.button.push");

    private static final @NotNull String COMMIT = Bundle.message("dialog.pending.button.commit");

    private final @NotNull List<Row> rowDifferences = new ArrayList<>();

    private final @NotNull Path repoRoot;

    private final @NotNull SelectionTable changes;

    private final @NotNull ChoiceInput branch;

    private final @NotNull TextInput message;

    private final @NotNull DialogSplitButton commit;

    private final @NotNull Consumer<Request> onCommit;

    private final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

    // Rule-SHARE-049
    public PendingChangesDialog(final @NotNull Project project, final @NotNull List<PendingChange> differences, final @NotNull Path repoRoot, final @NotNull List<String> branches, final @NotNull String currentBranch, final @NotNull Consumer<Request> onCommit) {
        super(project);
        this.repoRoot = repoRoot;
        this.onCommit = onCommit;

        title = Bundle.message("dialog.pending.title");

        final @NotNull DialogPart<SelectionTable> table = DialogPart.table()
                .column(Bundle.message("dialog.pending.column.change.type"), 150)
                .column(NodeType.TEST_SET.getDescription(), 150)
                .column(Bundle.message("caption.name"), 240)
                .column(Bundle.message("dialog.pending.column.before"), 180)
                .column(Bundle.message("dialog.pending.column.after"), 180)
                .build();
        final @NotNull DialogPart<ChoiceInput> branchRow =
                DialogPart.choice(Bundle.message("dialog.pending.caption.branch"), offered(branches, currentBranch), currentBranch);

        final @NotNull DialogPart<TextInput> messageField = DialogPart.textInput()
                .placeholder(Bundle.message("dialog.pending.placeholder"))
                .build();
        final @NotNull DialogPart<DialogSplitButton> commitButton =
                DialogPart.splitButton(PUSH, COMMIT);

        components = List.of(table, branchRow, messageField, commitButton);
        changes = table.getComponent();
        branch = branchRow.getComponent();
        message = messageField.getComponent();
        commit = commitButton.getComponent();

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, commit.getChosen(), this::submit),
                StatusBarShortcut.hint(Bundle.message("gesture.right.click"), Bundle.message("dialog.pending.hint.revert")),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;

        fillRows(differences);
        changes.selectAll();
        changes.onRowAction(Bundle.message("dialog.pending.revert.row"), this::revertRow);

        changes.onSelectionChanged(this::refreshCommit);
        refreshCommit();
    }

    private static @NotNull List<String> offered(final @NotNull List<String> branches, final @NotNull String current) {
        if (current.isEmpty() || branches.contains(current)) return branches;

        final @NotNull List<String> withCurrent = new ArrayList<>(branches);
        withCurrent.addFirst(current);

        return withCurrent;
    }

    // UC-SHARE-010, Rule-SHARE-045
    private void fillRows(final @NotNull List<PendingChange> differences) {
        for (final PendingChange diff : differences) {
            for (final FieldChange change : diff.fieldChanges()) {
                final Object @NotNull [] cells = {change.changeType().getLabel(), diff.testSet(), diff.name(), change.oldValue(), change.newValue()};
                // Rule-SHARE-129
                if (diff.subject() == ChangeSubject.MARKER)
                    changes.addFixedRow(Bundle.message("dialog.pending.marker.fixed"), cells);
                else changes.addRow(cells);
                rowDifferences.add(new Row(diff, change));
            }
        }
    }

    private @NotNull List<PendingChange> selectedDifferences() {
        final @NotNull Set<PendingChange> selected = new LinkedHashSet<>();
        for (final int row : changes.getSelectedRows()) {
            if (row < rowDifferences.size()) selected.add(rowDifferences.get(row).diff());
        }
        return List.copyOf(selected);
    }

    // UC-SHARE-011, Rule-SHARE-051, Rule-SHARE-119, Rule-SHARE-053
    private void revertRow(final int row) {
        if (row >= rowDifferences.size()) return;

        final @NotNull PendingChange diff = rowDifferences.get(row).diff();
        final @NotNull ChangeType changeType = rowDifferences.get(row).change().changeType();

        if (!diff.isRevertible()) {
            notifier.softRefuse(project, Bundle.message("dialog.pending.revert.only.test.case"));
            return;
        }

        try {
            final @NotNull Optional<Path> found = Optional.ofNullable(repoRoot.resolve(diff.relativeFilePath()).getParent());
            if (found.isEmpty()) return;

            final @NotNull Path testSetPath = found.orElseThrow();
            final @NotNull UUID testCaseUuid = UUID.fromString(diff.testCaseUuid());
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(project, testSetPath, List.of(testCaseUuid));

            final boolean reverted = switch (diff.gitStatusKind()) {
                case ADDED -> removeNewTestCase(testSetPath, testCaseUuid, before);
                // Rule-INTERNAL-035
                case DELETED -> testCases.putTestCaseVerbatim(testSetPath, diff.committed());
                case MODIFIED -> revertField(testSetPath, changeType, diff);
            };

            if (!reverted) return;

            final @NotNull TestCaseSnapshot after = TestCaseSnapshot.of(project, testSetPath, List.of(testCaseUuid));
            final @NotNull List<TestCase> named = before.present().isEmpty() ? after.present() : before.present();
            TestCaseSnapshot.record(project, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.revert"), named), before, after);

            removeRow(row);
            notifier.softShow(project, Done.REVERTED);

        } catch (final Exception ex) {
            notifier.error(project, Bundle.message("dialog.pending.revert.failed.title"), Bundle.message("dialog.pending.revert.failed.message", FailureText.of(ex)));
        }
    }

    // UC-SHARE-011, Rule-CODEGEN-049
    private boolean removeNewTestCase(final @NotNull Path testSetPath, final @NotNull UUID testCaseUuid, final @NotNull TestCaseSnapshot before) {
        if (!testCases.removeTestCase(testSetPath, testCaseUuid)) return false;

        CodeChangeType.REMOVE_TEST_CASE.executeAll(project, before.present());
        return true;
    }

    // UC-SHARE-011, Rule-SHARE-052
    private boolean revertField(final @NotNull Path testSetPath, final @NotNull ChangeType changeType, final @NotNull PendingChange diff) {
        if (!changeType.isRevertible()) {
            notifier.softRefuse(project, Bundle.message("dialog.pending.revert.not.supported", changeType.getLabel()));
            return false;
        }

        final @NotNull Optional<TestCase> current = testCases.findTestCase(UUID.fromString(diff.testCaseUuid()));
        if (current.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("dialog.pending.revert.gone"));
            return false;
        }

        final @NotNull TestCaseBuilder draft = current.orElseThrow().edit();
        final @NotNull TestCase committed = diff.committed();
        changeType.getRevertAction().apply(draft, committed);
        final @NotNull TestCase working = draft.build();

        if (TestCaseFieldChanges.compare(committed, working).isEmpty()) {
            working.takeAuditOf(committed);
            return testCases.putTestCaseVerbatim(testSetPath, working);
        }

        return testCases.putTestCase(testSetPath, working);
    }

    private void removeRow(final int row) {
        changes.removeRow(row);
        rowDifferences.remove(row);
        refreshCommit();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-080
    private void refreshCommit() {
        commit.enableUnless(changes.getSelectedRows().isEmpty()
                ? Optional.of(Bundle.message("dialog.pending.nothing.chosen"))
                : Optional.empty());
    }

    // UC-SHARE-012, UC-SHARE-013
    @Override
    protected void submit() {
        final @NotNull List<PendingChange> selected = selectedDifferences();
        if (selected.isEmpty()) return;

        final @NotNull String written = accepted(message);
        if (written.isEmpty()) return;

        onCommit.accept(new Request(selected, written,
                PUSH.equals(commit.getChosen()), branch.getValue(), branch.isNew()));
        closeOk();
    }
}
