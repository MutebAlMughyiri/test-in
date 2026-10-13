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

package org.testin.report.generators;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.dhatim.fastexcel.HyperLink;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.ReportColor;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.markers.DetailRow;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.FailureText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class TestRunExcelGenerator {
    private static @NotNull String orNotAvailable(final @NotNull String value) {
        return value.isEmpty() ? Bundle.message("report.overview.not.recorded") : value;
    }

    // Rule-REPORT-020
    private static void writeOverview(final @NotNull Worksheet ws, final @NotNull String projectName, final @NotNull TestRunNode testRunNode, final @NotNull TestRunSummary summary) {
        ws.value(0, 0, Bundle.message("report.title"));
        ws.style(0, 0).bold().fontSize(14).set();

        int row = heading(ws, 2, Bundle.message("report.heading.overview"));
        for (final DetailRow overview : ReportOverview.rowsFor(projectName, testRunNode, summary)) {
            ws.value(row, 0, overview.caption());
            ws.style(row, 0).bold().set();
            ws.value(row++, 1, overview.value());
        }

        row = heading(ws, row + 1, Bundle.message("report.heading.execution"));
        ws.value(row++, 0, Bundle.message("report.summary.named", testRunNode.getName(),
                String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"));
        for (final ReportTile figure : ReportTile.shownFor(summary)) {
            caption(ws, row, figure.getLabel(), figure.getHex());
            ws.value(row, 1, figure.amountIn(summary));
            if (!figure.getUnit().isEmpty()) ws.style(row, 1).format("0\"" + figure.getUnit() + "\"").set();
            row++;
        }

        if (TestRunResultAnalysis.anyWrittenIn(testRunNode.getMarker().getResultAnalysis())) {
            row = heading(ws, row + 1, Bundle.message("report.heading.analysis"));

            for (final TestRunResultAnalysis section : TestRunResultAnalysis.values()) {
                final @NotNull String written = section.writtenIn(testRunNode.getMarker().getResultAnalysis());
                if (written.isEmpty()) continue;

                caption(ws, row, section.heading(summary), section.getHexColor());
                ws.value(row, 1, written);
                ws.style(row++, 1).wrapText(true).set();
            }
        }

        ws.width(0, 32);
        ws.width(1, 80);
    }

    private static int heading(final @NotNull Worksheet ws, final int row, final @NotNull String text) {
        ws.value(row, 0, text);
        ws.style(row, 0).bold().fontSize(12).set();

        return row + 1;
    }

    private static void caption(final @NotNull Worksheet ws, final int row, final @NotNull String text, final @NotNull String hex) {
        ws.value(row, 0, text);
        ws.style(row, 0).bold().fontColor(hex).set();
    }

    // Rule-REPORT-019, Rule-REPORT-020
    private static void writeTestCases(final @NotNull Worksheet ws, final @NotNull RunItems runItems) {
        ws.value(0, 0, Bundle.message("report.excel.caption.id"));
        ws.value(0, 1, Bundle.message("caption.test.case"));
        ws.value(0, 2, TestRunEditorFields.RUN_STATUS.getName());
        ws.value(0, 3, TestRunEditorFields.ACTUAL_RESULT.getName());
        ws.value(0, 4, TestRunEditorFields.BUG_SEVERITY.getName());
        ws.value(0, 5, TestRunEditorFields.BUG_PRIORITY.getName());
        ws.value(0, 6, TestRunEditorFields.DURATION.getName());
        ws.value(0, 7, TestRunEditorFields.EXPECTED_RESULT.getName());
        ws.value(0, 8, TestRunEditorFields.BUG_ISSUE.getName());
        ws.range(0, 0, 0, 8).style().bold().fillColor(ReportColor.PANEL.hex()).set();

        int row = 1;
        for (final RunItem runItem : runItems.getAll()) {
            ProgressManager.checkCanceled();
            final @NotNull UUID testCaseId = runItem.getId();
            final @NotNull TestCaseDto details = runItem.shownTestCase();

            ws.value(row, 0, testCaseId.toString());
            ws.value(row, 1, orNotAvailable(details.getDescription()));
            ws.value(row, 2, runItem.shownStatus().getLabel());
            // Rule-REPORT-027
            ws.value(row, 3, ReportText.joined("\n", runItem.getActualResult(), ReportText.otherExecutions(runItem).orElse("")));
            ws.value(row, 4, TestRunEditorFields.BUG_SEVERITY.getRunItemValueExtractor().apply(runItem));
            ws.value(row, 5, TestRunEditorFields.BUG_PRIORITY.getRunItemValueExtractor().apply(runItem));
            ws.value(row, 6, Display.formatDuration(runItem.getDuration()));
            ws.value(row, 7, orNotAvailable(details.getExpectedResult()));

            final @NotNull ReportSection runItemStatus = ReportSection.of(runItem);
            ws.range(row, 0, row, 8).style().fillColor(runItemStatus.getHexColor()).fontColor(runItemStatus.textHex()).wrapText(true).set();

            final int line = row;
            runItem.bugIssue().ifPresent(url -> {
                ws.hyperlink(line, 8, HyperLink.external(url, BugIssueUrl.shortReference(url)));
                ws.style(line, 8).fillColor(runItemStatus.getHexColor()).fontColor(runItemStatus.textHex()).underlined().set();
            });

            row++;
        }

        ws.width(0, 40);
        ws.width(1, 30);
        ws.width(2, 15);
        ws.width(3, 30);
        ws.width(4, 15);
        ws.width(5, 15);
        ws.width(6, 15);
        ws.width(7, 40);
        ws.width(8, 15);
    }

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-020
    public byte @NotNull [] generate(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final @NotNull Workbook wb = new Workbook(os, Bundle.getPluginName(), "1.0");
            final @NotNull TestRunSummary summary = TestRunSummary.of(runItems.getAll());

            writeOverview(wb.newWorksheet(Bundle.message("report.excel.sheet.overview")), Services.getInstance(project, BoundTestProject.class).name(), testRunNode, summary);
            writeTestCases(wb.newWorksheet(Bundle.message("report.excel.sheet.test.cases")), runItems);

            wb.finish();

            return os.toByteArray();
        } catch (final IOException ex) {
            TestinLogger.error("Excel report generation failed: " + FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }
}