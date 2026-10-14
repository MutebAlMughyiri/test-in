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
import org.testin.model.TestCase;
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
    private static void writeOverview(final @NotNull Worksheet worksheet, final @NotNull String testProjectName, final @NotNull TestRunNode testRunNode, final @NotNull TestRunSummary summary) {
        worksheet.value(0, 0, Bundle.message("report.title"));
        worksheet.style(0, 0).bold().fontSize(14).set();

        int row = heading(worksheet, 2, Bundle.message("report.heading.overview"));
        for (final DetailRow overview : ReportOverview.rowsFor(testProjectName, testRunNode, summary)) {
            worksheet.value(row, 0, overview.caption());
            worksheet.style(row, 0).bold().set();
            worksheet.value(row++, 1, overview.value());
        }

        row = heading(worksheet, row + 1, Bundle.message("report.heading.execution"));
        worksheet.value(row++, 0, Bundle.message("report.summary.named", testRunNode.getName(),
                String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"));
        for (final ReportTile figure : ReportTile.shownFor(summary)) {
            caption(worksheet, row, figure.labelIn(testRunNode.getMarker().getStatus()), figure.getHex());
            worksheet.value(row, 1, figure.amountIn(summary));
            if (!figure.getUnit().isEmpty()) worksheet.style(row, 1).format("0\"" + figure.getUnit() + "\"").set();
            row++;
        }

        if (TestRunResultAnalysis.anyWrittenIn(testRunNode.getMarker().getResultAnalysis())) {
            row = heading(worksheet, row + 1, Bundle.message("report.heading.analysis"));

            for (final TestRunResultAnalysis section : TestRunResultAnalysis.values()) {
                final @NotNull String written = section.writtenIn(testRunNode.getMarker().getResultAnalysis());
                if (written.isEmpty()) continue;

                caption(worksheet, row, section.heading(summary, testRunNode.getMarker().getStatus()), section.getHexColor());
                worksheet.value(row, 1, written);
                worksheet.style(row++, 1).wrapText(true).set();
            }
        }

        worksheet.width(0, 32);
        worksheet.width(1, 80);
    }

    private static int heading(final @NotNull Worksheet worksheet, final int row, final @NotNull String text) {
        worksheet.value(row, 0, text);
        worksheet.style(row, 0).bold().fontSize(12).set();

        return row + 1;
    }

    private static void caption(final @NotNull Worksheet worksheet, final int row, final @NotNull String text, final @NotNull String hex) {
        worksheet.value(row, 0, text);
        worksheet.style(row, 0).bold().fontColor(hex).set();
    }

    // Rule-REPORT-019, Rule-REPORT-020
    private static void writeTestCases(final @NotNull Worksheet worksheet, final @NotNull RunItems runItems) {
        worksheet.value(0, 0, Bundle.message("report.excel.caption.uuid"));
        worksheet.value(0, 1, Bundle.message("caption.test.case"));
        worksheet.value(0, 2, TestRunEditorFields.RUN_STATUS.getName());
        worksheet.value(0, 3, TestRunEditorFields.ACTUAL_RESULT.getName());
        worksheet.value(0, 4, TestRunEditorFields.BUG_SEVERITY.getName());
        worksheet.value(0, 5, TestRunEditorFields.BUG_PRIORITY.getName());
        worksheet.value(0, 6, TestRunEditorFields.DURATION.getName());
        worksheet.value(0, 7, TestRunEditorFields.EXPECTED_RESULT.getName());
        worksheet.value(0, 8, TestRunEditorFields.BUG_ISSUE.getName());
        worksheet.range(0, 0, 0, 8).style().bold().fillColor(ReportColor.PANEL.hex()).set();

        int row = 1;
        for (final RunItem runItem : runItems.getAll()) {
            ProgressManager.checkCanceled();
            final @NotNull UUID testCaseUuid = runItem.getUuid();
            final @NotNull TestCase details = runItem.shownTestCase();

            worksheet.value(row, 0, testCaseUuid.toString());
            worksheet.value(row, 1, orNotAvailable(details.getDescription()));
            worksheet.value(row, 2, runItem.getStatus().getLabel());
            // Rule-REPORT-027
            worksheet.value(row, 3, ReportText.joined("\n", runItem.shownExecution().getActualResult(), ReportText.otherExecutions(runItem).orElse("")));
            worksheet.value(row, 4, TestRunEditorFields.BUG_SEVERITY.getRunItemValueExtractor().apply(runItem));
            worksheet.value(row, 5, TestRunEditorFields.BUG_PRIORITY.getRunItemValueExtractor().apply(runItem));
            worksheet.value(row, 6, Display.formatDuration(runItem.shownExecution().getDuration()));
            worksheet.value(row, 7, orNotAvailable(details.getExpectedResult()));

            final @NotNull ReportSection runItemStatus = ReportSection.of(runItem);
            worksheet.range(row, 0, row, 8).style().fillColor(runItemStatus.getHexColor()).fontColor(runItemStatus.textHex()).wrapText(true).set();

            final int line = row;
            runItem.bugIssue().ifPresent(url -> {
                worksheet.hyperlink(line, 8, HyperLink.external(url, BugIssueUrl.shortReference(url)));
                worksheet.style(line, 8).fillColor(runItemStatus.getHexColor()).fontColor(runItemStatus.textHex()).underlined().set();
            });

            row++;
        }

        worksheet.width(0, 40);
        worksheet.width(1, 30);
        worksheet.width(2, 15);
        worksheet.width(3, 30);
        worksheet.width(4, 15);
        worksheet.width(5, 15);
        worksheet.width(6, 15);
        worksheet.width(7, 40);
        worksheet.width(8, 15);
    }

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-020
    public byte @NotNull [] generate(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final @NotNull Workbook workbook = new Workbook(output, Bundle.getPluginName(), "1.0");
            final @NotNull TestRunSummary summary = TestRunSummary.of(runItems.getAll());

            writeOverview(workbook.newWorksheet(Bundle.message("report.excel.sheet.overview")), Services.getInstance(project, BoundTestProject.class).name(), testRunNode, summary);
            writeTestCases(workbook.newWorksheet(Bundle.message("report.excel.sheet.test.cases")), runItems);

            workbook.finish();

            return output.toByteArray();
        } catch (final IOException ex) {
            TestinLogger.error("Excel report generation failed: " + FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }
}