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
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportColor;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.markers.DetailRow;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunSummary;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Bundle;
import org.testin.util.ReportFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TestRunExcelGenerator {
    private static final int CHARACTER = 256;

    private final @NotNull Map<String, XSSFCellStyle> styles = new HashMap<>();

    private static @NotNull XSSFColor color(final @NotNull String hex) {
        return new XSSFColor(HexFormat.of().parseHex(hex), null);
    }

    private static @NotNull Cell cell(final @NotNull Sheet sheet, final int row, final int column) {
        return Optional.ofNullable(sheet.getRow(row)).orElseGet(() -> sheet.createRow(row)).createCell(column);
    }

    private static void widths(final @NotNull Sheet sheet, final int @NotNull ... characters) {
        for (int column = 0; column < characters.length; column++)
            sheet.setColumnWidth(column, characters[column] * CHARACTER);
    }

    // Rule-REPORT-019, Rule-REPORT-020
    private static @NotNull List<TestRunEditorFields> testCaseColumns() {
        final @NotNull List<TestRunEditorFields> columns = new ArrayList<>(List.of(TestRunEditorFields.RUN_STATUS, TestRunEditorFields.ACTUAL_RESULT));
        ReportSection.FAILED.getFailureDetailColumns().forEach(column -> columns.add(column.getField()));
        columns.addAll(List.of(TestRunEditorFields.DURATION, TestRunEditorFields.EXPECTED_RESULT, TestRunEditorFields.BUG_ISSUE));
        return columns;
    }

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-020
    public byte @NotNull [] generate(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final @NotNull TestRunSummary summary = TestRunSummary.of(runItems.getAll());

            writeOverview(workbook, workbook.createSheet(Bundle.message("report.excel.sheet.overview")), Services.getInstance(project, BoundTestProject.class).name(), testRunNode, summary);
            writeTestCases(workbook, workbook.createSheet(Bundle.message("report.excel.sheet.test.cases")), runItems);

            workbook.write(output);
            return output.toByteArray();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // Rule-REPORT-020
    private void writeOverview(final @NotNull XSSFWorkbook workbook, final @NotNull Sheet sheet, final @NotNull String testProjectName, final @NotNull TestRunNode testRunNode, final @NotNull TestRunSummary summary) {
        write(sheet, 0, 0, Bundle.message("report.title"), style(workbook, true, 14, ReportColor.INK.hex(), "", false));

        int row = heading(workbook, sheet, 2, Bundle.message("report.heading.overview"));
        for (final DetailRow overview : ReportOverview.rowsFor(testProjectName, testRunNode, summary)) {
            write(sheet, row, 0, overview.caption(), style(workbook, true, 0, ReportColor.INK.hex(), "", false));
            cell(sheet, row++, 1).setCellValue(overview.value());
        }

        row = heading(workbook, sheet, row + 1, Bundle.message("report.heading.execution"));
        cell(sheet, row++, 0).setCellValue(ReportText.summary(testRunNode.getName(), summary, value -> value));
        for (final ReportTile figure : ReportTile.shownFor(summary)) {
            write(sheet, row, 0, figure.labelIn(testRunNode.getMarker().getStatus()), style(workbook, true, 0, figure.getHex(), "", false));
            final @NotNull Cell amount = cell(sheet, row++, 1);
            amount.setCellValue(figure.amountIn(summary).doubleValue());
            if (!figure.getUnit().isEmpty()) amount.setCellStyle(unitStyle(workbook, figure.getUnit()));
        }

        final @NotNull Map<TestRunResultAnalysis, String> written = TestRunResultAnalysis.shown(testRunNode.getMarker().getResultAnalysis());
        if (!written.isEmpty()) {
            row = heading(workbook, sheet, row + 1, Bundle.message("report.heading.analysis"));

            for (final Map.Entry<TestRunResultAnalysis, String> section : written.entrySet()) {
                write(sheet, row, 0, section.getKey().heading(summary, testRunNode.getMarker().getStatus()), style(workbook, true, 0, section.getKey().getHexColor(), "", false));
                write(sheet, row++, 1, section.getValue(), style(workbook, false, 0, ReportColor.INK.hex(), "", true));
            }
        }

        widths(sheet, 32, 80);
    }

    private int heading(final @NotNull XSSFWorkbook workbook, final @NotNull Sheet sheet, final int row, final @NotNull String text) {
        write(sheet, row, 0, text, style(workbook, true, 12, ReportColor.INK.hex(), "", false));
        return row + 1;
    }

    // Rule-REPORT-019, Rule-REPORT-020, Rule-REPORT-027
    private void writeTestCases(final @NotNull XSSFWorkbook workbook, final @NotNull Sheet sheet, final @NotNull RunItems runItems) {
        final @NotNull List<TestRunEditorFields> columns = testCaseColumns();
        final @NotNull XSSFCellStyle header = style(workbook, true, 0, ReportColor.INK.hex(), ReportColor.PANEL.hex(), false);
        write(sheet, 0, 0, Bundle.message("report.excel.caption.uuid"), header);
        write(sheet, 0, 1, Bundle.message("caption.test.case"), header);
        for (int column = 0; column < columns.size(); column++)
            write(sheet, 0, 2 + column, columns.get(column).getName(), header);

        int row = 1;
        for (final RunItem runItem : runItems.getAll()) {
            ProgressManager.checkCanceled();
            final @NotNull ReportSection section = ReportSection.of(runItem);
            final @NotNull XSSFCellStyle shaded = style(workbook, false, 0, section.textHex(), section.getHexColor(), true);

            write(sheet, row, 0, runItem.getUuid().toString(), shaded);
            write(sheet, row, 1, ReportText.testCaseName(runItem), shaded);
            for (int column = 0; column < columns.size(); column++) {
                write(sheet, row, 2 + column, valueOf(columns.get(column), runItem), shaded);
            }

            final int line = row;
            runItem.shownExecution().bugIssue().ifPresent(url -> {
                final @NotNull Cell link = cell(sheet, line, 2 + columns.indexOf(TestRunEditorFields.BUG_ISSUE));
                final @NotNull Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(url);
                link.setHyperlink(hyperlink);
                link.setCellValue(BugIssueUrl.shortReference(url));
                link.setCellStyle(style(workbook, false, 0, section.textHex(), section.getHexColor(), true, true));
            });

            row++;
        }

        widths(sheet, 40, 30, 15, 30, 15, 15, 15, 40, 15);
    }

    // Rule-REPORT-019, Rule-REPORT-027
    private static @NotNull String valueOf(final @NotNull TestRunEditorFields column, final @NotNull RunItem runItem) {
        final @NotNull String value = column == TestRunEditorFields.ACTUAL_RESULT
                ? ReportText.joined("\n", runItem.shownExecution().getActualResult(), ReportText.otherExecutions(runItem).orElse(""))
                : column.gridValue(runItem);
        return ReportText.orEmpty(value);
    }

    private static void write(final @NotNull Sheet sheet, final int row, final int column, final @NotNull String value, final @NotNull XSSFCellStyle style) {
        final @NotNull Cell cell = cell(sheet, row, column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private @NotNull XSSFCellStyle unitStyle(final @NotNull XSSFWorkbook workbook, final @NotNull String unit) {
        return styles.computeIfAbsent("unit:" + unit, _ -> {
            final @NotNull XSSFCellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat("0\"" + unit + "\""));
            return style;
        });
    }

    private @NotNull XSSFCellStyle style(final @NotNull XSSFWorkbook workbook, final boolean bold, final int points, final @NotNull String ink, final @NotNull String fill, final boolean wraps) {
        return style(workbook, bold, points, ink, fill, wraps, false);
    }

    private @NotNull XSSFCellStyle style(final @NotNull XSSFWorkbook workbook, final boolean bold, final int points, final @NotNull String ink, final @NotNull String fill, final boolean wraps, final boolean underlined) {
        return styles.computeIfAbsent(bold + ":" + points + ":" + ink + ":" + fill + ":" + wraps + ":" + underlined, _ -> {
            final @NotNull XSSFFont font = workbook.createFont();
            font.setFontName(ReportFont.FAMILY);
            font.setBold(bold);
            if (points > 0) font.setFontHeightInPoints((short) points);
            font.setColor(color(ink));
            if (underlined) font.setUnderline(XSSFFont.U_SINGLE);

            final @NotNull XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setWrapText(wraps);
            if (!fill.isEmpty()) {
                style.setFillForegroundColor(color(fill));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return style;
        });
    }
}
