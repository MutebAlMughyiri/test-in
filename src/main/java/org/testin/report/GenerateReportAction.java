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

package org.testin.report;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractBoundTestProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.editor.TestinEditor;
import org.testin.explorer.tree.TreeValues;
import org.testin.git.history.TestRunFromGit;
import org.testin.importexport.FileFormat;
import org.testin.importexport.exports.ExportNotice;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Supplier;

public class GenerateReportAction extends AbstractBoundTestProjectAction {
    private final @NotNull Supplier<Optional<TestRunNode>> selectedTestRun;
    private final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
    private final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

    public GenerateReportAction(final @NotNull Project project, final @NotNull SimpleTree tree) {
        super(project, Bundle.message("report.action.text"), Bundle.message("report.action.description"), AllIcons.ToolbarDecorator.Export);
        this.selectedTestRun = () -> TreeValues.valueOf(tree.getLastSelectedPathComponent(), TestRunNode.class);
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), tree);
    }

    public GenerateReportAction(final @NotNull Project project, final @NotNull TestinEditor editor) {
        super(project, Bundle.message("report.action.text"), Bundle.message("report.action.description"), AllIcons.Actions.Report);
        this.selectedTestRun = editor::shownTestRun;
    }

    public GenerateReportAction(final @NotNull Project project, final @NotNull TestinEditor editor, final @NotNull JBList<TestCase> list) {
        this(project, editor);
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), list);
    }

    private static void write(final @NotNull File outputFile, final byte @NotNull [] content) {
        try {
            Files.write(outputFile.toPath(), content);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // UC-REPORT-001
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    // UC-REPORT-001, Rule-REPORT-001
    @Override
    public void update(final @NotNull AnActionEvent e) {
        GrayWithReason.unless(this, e, isAvailable(), selectedTestRun.get()
                .map(testRun -> Bundle.message("toolbar.report.disabled", testRun.getMarker().getStatus().getLabel()))
                .orElseGet(() -> Bundle.message("report.select.test.run.description")));
    }

    // UC-REPORT-001, Rule-REPORT-016
    public boolean isAvailable() {
        return selectedTestRun.get().map(testRun -> testRun.getMarker().getStatus().isReportable()).orElse(false);
    }

    // UC-REPORT-001
    public void execute() {
        selectedTestRun.get().ifPresent(testRun -> new GenerateReportDialog(project,
                ReportFileName.suggestedFor(project, testRun, ZonedDateTime.now(ZoneId.systemDefault())),
                (format, file) -> processAndSave(testRun, format, file)).show());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    // UC-REPORT-001, Rule-REPORT-003
    private void processAndSave(final @NotNull TestRunNode testRun, final @NotNull FileFormat format, final @NotNull File outputFile) {
        BackgroundWork.run(project, Bundle.message("report.task.generating", format.getLabel(), testRun.getName()), Bundle.message("report.failed.title", format.getLabel()), indicator -> writeReport(testRun, format, outputFile, indicator));
    }

    // UC-REPORT-001, Rule-REPORT-003
    void writeReport(final @NotNull TestRunNode testRun, final @NotNull FileFormat format, final @NotNull File outputFile, final @NotNull ProgressIndicator indicator) {
        final @NotNull Path directory = testRun.getPath();

        // Rule-EDITOR-PANEL-239
        TestRunFromGit.read(project, directory);
        final @NotNull RunItems runItems = testRuns.getRunItems(directory);

        final byte[] fileBytes = format.generateReport(project, testRun, runItems);

        // Rule-REPORT-003
        indicator.checkCanceled();

        write(outputFile, fileBytes);

        notifier.infoWithActions(project,
                Bundle.message("report.generated.title", format.getLabel()),
                Bundle.message("report.generated.message", outputFile.getName()),
                notifier.action(Bundle.message("report.open"), () -> ExportNotice.open(project, outputFile)),
                ExportNotice.copyPath(project, outputFile)
        );
    }
}
