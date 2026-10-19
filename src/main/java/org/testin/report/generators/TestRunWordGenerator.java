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
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.testin.model.ReportColor;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.markers.DetailRow;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunSummary;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.ReportFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class TestRunWordGenerator {
    private static final @NotNull String NO_BORDER = "";

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public byte @NotNull [] generate(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            try (XWPFDocument document = new XWPFDocument()) {
                final @NotNull String testProjectName = Services.getInstance(project, BoundTestProject.class).name();

                addText(document, Bundle.message("report.title"), ReportFont.TITLE.roundedPoints(), true, ReportColor.HEADING.hex(), NO_BORDER, 2);

                addText(document, ReportText.subtitle(testProjectName, testRunNode.getMarker()),
                        ReportFont.SUBTITLE.roundedPoints(), false, ReportColor.ACCENT.hex(), NO_BORDER, 0);
                addText(document, testRunNode.getName(), ReportFont.LEAD.roundedPoints(), false, ReportColor.ACCENT.hex(), ReportColor.HEADING.hex(), 1);

                final @NotNull XWPFParagraph confidentialNotice = addText(document, Bundle.message("report.confidential"), ReportFont.CAPTION.roundedPoints(), false, ReportColor.MUTED.hex(), NO_BORDER, 20);
                setItalic(confidentialNotice);

                addHeading(document, Bundle.message("report.heading.overview"), 0, 15);

                final @NotNull TestRunSummary summary = TestRunSummary.of(runItems.getAll());
                final @NotNull TestRunStatus testRunStatus = testRunNode.getMarker().getStatus();

                final @NotNull XWPFTable overviewTable = document.createTable(1, 2);
                overviewTable.setWidth("100%");
                overviewTable.setWidthType(TableWidthType.PCT);
                setTableWidths(overviewTable, 30, 70);

                int overviewRow = 0;
                for (final DetailRow row : ReportOverview.rowsFor(testProjectName, testRunNode, summary)) {
                    addOverviewRow(overviewTable, overviewRow++, row.caption(), row.value());
                }

                setTableBorders(overviewTable);

                addHeading(document, Bundle.message("report.heading.execution"), 20, 12);

                addText(document, ReportText.summary(testRunNode.getName(), summary, UnaryOperator.identity()),
                        ReportFont.LEAD.roundedPoints(), false, ReportColor.INK.hex(), NO_BORDER, 12);

                final @NotNull List<ReportTile> headline = ReportTile.shownFor(summary);
                final int tiles = headline.size();

                final @NotNull XWPFTable statsTable = document.createTable(1, tiles);
                statsTable.setWidth("100%");
                statsTable.setWidthType(TableWidthType.PCT);
                setTableBorders(statsTable);
                setTableWidths(statsTable, evenWidths(tiles));

                for (int tile = 0; tile < tiles; tile++) {
                    final @NotNull ReportTile figure = headline.get(tile);
                    addStatisticCell(statsTable, tile, figure.valueIn(summary), figure.labelIn(testRunStatus), figure.getHex());
                }

                final @NotNull Map<TestRunResultAnalysis, String> analysis = TestRunResultAnalysis.shown(testRunNode.getMarker().getResultAnalysis());
                final boolean analyzed = !analysis.isEmpty();

                if (analyzed) {
                    addHeading(document, Bundle.message("report.heading.analysis"), 20, 12);

                    for (final Map.Entry<TestRunResultAnalysis, String> written : analysis.entrySet()) {
                        addColoredCount(document, written.getKey().heading(summary, testRunStatus), written.getKey().getHexColor());
                        addText(document, written.getValue(), ReportFont.BODY.roundedPoints(), false, ReportColor.INK.hex(), NO_BORDER, 8);
                    }
                }

                for (final NumberedSection numbered : ReportText.numberedSections(summary, analyzed)) {
                    final @NotNull ReportSection section = numbered.section();
                    buildTestCaseTable(document, String.valueOf(numbered.number()), section, section.titleIn(testRunStatus), section.description(String.valueOf(numbered.count())), runItems);
                }

                addFooter(document);

                applyPageMargins(document);

                document.write(bytes);
            }
            return bytes.toByteArray();

        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private @NotNull XWPFParagraph addText(final @NotNull XWPFDocument document, final @NotNull String text, final int size, final boolean bold, final @NotNull String color, final @NotNull String bottomBorder, final int spacingAfterPoints) {
        final @NotNull XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(spacingAfterPoints * 20);
        final @NotNull XWPFRun textRun = paragraph.createRun();
        writeLines(textRun, text, false);
        textRun.setFontSize(size);
        textRun.setFontFamily(ReportFont.FAMILY);
        textRun.setBold(bold);
        textRun.setColor(color);
        if (!bottomBorder.isEmpty()) {
            final @NotNull CTBorder bottom = paragraph.getCTPPr().addNewPBdr().addNewBottom();
            bottom.setVal(STBorder.Enum.forString("single"));
            bottom.setSz(BigInteger.valueOf(16));
            bottom.setColor(bottomBorder);
        }
        return paragraph;
    }

    private void setItalic(final @NotNull XWPFParagraph paragraph) {
        for (final XWPFRun textRun : paragraph.getRuns()) {
            textRun.setItalic(true);
        }
    }

    private void addHeading(final @NotNull XWPFDocument document, final @NotNull String text, final int pointsBefore, final int pointsAfter) {
        final @NotNull XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(pointsBefore * 20);
        paragraph.setSpacingAfter(pointsAfter * 20);
        final @NotNull XWPFRun textRun = paragraph.createRun();
        textRun.setText(text);
        textRun.setFontSize(ReportFont.SECTION.roundedPoints());
        textRun.setFontFamily(ReportFont.FAMILY);
        textRun.setBold(true);
        textRun.setColor(ReportColor.HEADING.hex());
        final @NotNull CTBorder headingBottom = paragraph.getCTPPr().addNewPBdr().addNewBottom();
        headingBottom.setVal(STBorder.Enum.forString("single"));
        headingBottom.setSz(BigInteger.valueOf(8));
        headingBottom.setColor(ReportColor.HEADING.hex());
    }

    private void addOverviewRow(final @NotNull XWPFTable table, final int rowIndex, final @NotNull String label, final @NotNull String value) {
        final @NotNull XWPFTableRow row = rowIndex == 0 ? table.getRow(0) : table.createRow();
        final @NotNull XWPFTableCell labelCell = row.getCell(0);
        final @NotNull XWPFTableCell valueCell = row.getCell(1);
        shadeCell(labelCell, ReportColor.PANEL.hex());
        setCellPadding(labelCell, 4, 8, 4, 8);
        setCellPadding(valueCell, 4, 8, 4, 8);
        setCellText(labelCell, label, ReportFont.HEADING.roundedPoints(), true, ReportColor.HEADING.hex());
        setCellText(valueCell, value, ReportFont.BODY.roundedPoints(), false, ReportColor.INK.hex());
    }

    private void addStatisticCell(final @NotNull XWPFTable table, final int column, final @NotNull String number, final @NotNull String label, final @NotNull String numberColor) {
        final @NotNull XWPFTableRow row = table.getRow(0);
        final @NotNull XWPFTableCell cell = row.getCell(column);
        shadeCell(cell, ReportColor.PANEL.hex());
        setCellPadding(cell, 8, 6, 8, 6);
        setCellText(cell, number, ReportFont.FIGURE.roundedPoints(), true, numberColor);

        cell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

        final @NotNull XWPFParagraph labelParagraph = cell.addParagraph();
        labelParagraph.setAlignment(ParagraphAlignment.CENTER);
        labelParagraph.setSpacingBefore(80);
        final @NotNull XWPFRun labelTextRun = labelParagraph.createRun();
        labelTextRun.setText(label);
        labelTextRun.setFontSize(ReportFont.SMALL.roundedPoints());
        labelTextRun.setFontFamily(ReportFont.FAMILY);
        labelTextRun.setBold(true);
        labelTextRun.setColor(ReportColor.MUTED.hex());
    }

    private void addColoredCount(final @NotNull XWPFDocument document, final @NotNull String heading, final @NotNull String headingColor) {
        final @NotNull XWPFParagraph headingParagraph = document.createParagraph();
        headingParagraph.setSpacingAfter(120);

        final @NotNull XWPFRun headingTextRun = headingParagraph.createRun();
        headingTextRun.setText(heading);
        headingTextRun.setFontSize(ReportFont.LEAD.roundedPoints());
        headingTextRun.setFontFamily(ReportFont.FAMILY);
        headingTextRun.setBold(true);
        headingTextRun.setColor(headingColor);
    }

    private void buildTestCaseTable(final @NotNull XWPFDocument document, final @NotNull String sectionNumber, final @NotNull ReportSection section, final @NotNull String title, final @NotNull String description, final @NotNull RunItems runItems) {
        addHeading(document, sectionNumber + ". " + title, 20, 12);
        addText(document, description, ReportFont.LEAD.roundedPoints(), false, ReportColor.INK.hex(), NO_BORDER, 12);

        final @NotNull List<FailureColumn> failureDetail = section.getFailureDetailColumns();
        final @NotNull XWPFTable table = document.createTable(1, 2 + failureDetail.size());
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        final @NotNull String headerBackground = section.getHexColor();
        final @NotNull String headerForeground = section.textHex();
        final @NotNull XWPFTableRow headerRow = table.getRow(0);
        addTestCaseHeader(headerRow, 0, "#", headerBackground, headerForeground);
        addTestCaseHeader(headerRow, 1, Bundle.message("caption.test.case"), headerBackground, headerForeground);
        int column = 2;
        for (final FailureColumn failureColumn : failureDetail) {
            addTestCaseHeader(headerRow, column++, failureColumn.getField().getName(), headerBackground, headerForeground);
        }

        int index = 1;
        boolean shadedRow = true;
        for (final RunItem runItem : runItems.getAll()) {
            if (!section.matches(runItem)) continue;
            ProgressManager.checkCanceled();

            final @NotNull String rowBackground = shadedRow ? ReportColor.PANEL.hex() : ReportColor.PAGE.hex();
            shadedRow = !shadedRow;

            final @NotNull XWPFTableRow row = table.createRow();
            final @NotNull XWPFTableCell numberCell = row.getCell(0);
            shadeCell(numberCell, rowBackground);
            setCellPadding(numberCell, 4, 6, 4, 6);
            setCellText(numberCell, String.valueOf(index), ReportFont.BODY.roundedPoints(), false, ReportColor.MUTED.hex());
            numberCell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

            final @NotNull XWPFTableCell testCaseCell = row.getCell(1);
            shadeCell(testCaseCell, rowBackground);
            setCellPadding(testCaseCell, 4, 6, 4, 6);
            final @NotNull String shownTestCaseName = ReportText.testCaseName(runItem);
            setCellText(testCaseCell, shownTestCaseName, ReportFont.BODY.roundedPoints(), false, ReportColor.INK.hex());

            // Rule-REPORT-027
            ReportText.otherExecutions(runItem).ifPresent(others -> styledTextRun(testCaseCell.addParagraph().createRun(), others, ReportFont.SMALL, ReportColor.MUTED.hex()));

            if (section.isWithFailureDetail()) addFailureDetail(row, section, runItem, rowBackground);

            index++;
        }

        setTableBorders(table);
        autoFitToContent(table);
    }

    private void addFailureDetail(final @NotNull XWPFTableRow row, final @NotNull ReportSection section, final @NotNull RunItem runItem, final @NotNull String rowBackground) {
        final @NotNull String actualResult = runItem.shownExecution().getActualResult();
        final @NotNull XWPFParagraph actualResultParagraph = row.getCell(1).addParagraph();
        styledTextRun(actualResultParagraph.createRun(), Bundle.message("report.actual.result", ReportText.orEmpty(actualResult)), ReportFont.SMALL, ReportColor.MUTED.hex());

        runItem.shownExecution().bugIssue().ifPresent(url -> {
            styledTextRun(actualResultParagraph.createRun(), " (", ReportFont.SMALL, ReportColor.MUTED.hex());
            final @NotNull XWPFHyperlinkRun issue = actualResultParagraph.createHyperlinkRun(url);
            styledTextRun(issue, BugIssueUrl.shortReference(url), ReportFont.SMALL, ReportColor.LINK.hex());
            issue.setUnderline(UnderlinePatterns.SINGLE);
            styledTextRun(actualResultParagraph.createRun(), ")", ReportFont.SMALL, ReportColor.MUTED.hex());
        });

        int at = 2;
        for (final FailureColumn column : section.getFailureDetailColumns()) {
            final @NotNull XWPFTableCell cell = row.getCell(at++);
            shadeCell(cell, rowBackground);
            setCellPadding(cell, 4, 6, 4, 6);
            setCellText(cell, column.textOf(runItem), ReportFont.BODY.roundedPoints(), true, column.emphasisOf(runItem).getHexColor());
        }
    }

    private void autoFitToContent(final @NotNull XWPFTable table) {
        final @NotNull CTTblPr properties = table.getCTTbl().getTblPr();
        final @NotNull CTTblLayoutType layout =
                properties.isSetTblLayout() ? properties.getTblLayout() : properties.addNewTblLayout();
        layout.setType(STTblLayoutType.AUTOFIT);

        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcPr cellProperties = propertiesOf(cell);
                (cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW())
                        .setType(STTblWidth.AUTO);
            }
        }
    }

    private void addTestCaseHeader(final @NotNull XWPFTableRow headerRow, final int column, final @NotNull String text, final @NotNull String backgroundColor, final @NotNull String textColor) {
        final @NotNull XWPFTableCell cell = headerRow.getCell(column);
        shadeCell(cell, backgroundColor);
        setCellPadding(cell, 5, 6, 5, 6);
        setCellText(cell, text, ReportFont.HEADING.roundedPoints(), true, textColor);
    }

    // UC-REPORT-001, Rule-REPORT-023
    private void writeLines(final @NotNull XWPFRun textRun, final @NotNull String text, final boolean replaceFirst) {
        final @NotNull List<String> lines = text.lines().toList();

        if (lines.isEmpty()) {
            textRun.setText("", 0);
            return;
        }

        if (replaceFirst) textRun.setText(lines.getFirst(), 0);
        else textRun.setText(lines.getFirst());

        for (final String line : lines.subList(1, lines.size())) {
            textRun.addBreak();
            textRun.setText(line);
        }
    }

    private void setCellText(final @NotNull XWPFTableCell cell, final @NotNull String text, final int size, final boolean bold, final @NotNull String color) {
        final @NotNull XWPFParagraph paragraph = cell.getParagraphs().getFirst();
        final boolean fresh = paragraph.getRuns().isEmpty();
        final @NotNull XWPFRun textRun = fresh ? paragraph.createRun() : paragraph.getRuns().getFirst();
        writeLines(textRun, text, !fresh);
        textRun.setFontSize(size);
        textRun.setFontFamily(ReportFont.FAMILY);
        textRun.setBold(bold);
        textRun.setColor(color);
    }

    private void shadeCell(final @NotNull XWPFTableCell cell, final @NotNull String hex) {
        propertiesOf(cell).addNewShd().setFill(hex);
    }

    private void setCellPadding(final @NotNull XWPFTableCell cell, final int topPoints, final int leftPoints, final int bottomPoints, final int rightPoints) {
        final @NotNull CTTcMar margins = propertiesOf(cell).addNewTcMar();
        final @NotNull CTTblWidth top = margins.addNewTop();
        top.setW(topPoints * 20);
        top.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth left = margins.addNewLeft();
        left.setW(leftPoints * 20);
        left.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth bottom = margins.addNewBottom();
        bottom.setW(bottomPoints * 20);
        bottom.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth right = margins.addNewRight();
        right.setW(rightPoints * 20);
        right.setType(STTblWidth.Enum.forString("dxa"));
    }

    private @NotNull CTTcPr propertiesOf(final @NotNull XWPFTableCell cell) {
        final @NotNull CTTc rawCell = cell.getCTTc();
        return rawCell.isSetTcPr() ? rawCell.getTcPr() : rawCell.addNewTcPr();
    }

    private void setTableBorders(final @NotNull XWPFTable table) {
        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcBorders borders = propertiesOf(cell).addNewTcBorders();
                final @NotNull CTBorder top = borders.addNewTop();
                top.setColor(ReportColor.LINE.hex());
                top.setSz(BigInteger.valueOf(4));
                top.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder bottom = borders.addNewBottom();
                bottom.setColor(ReportColor.LINE.hex());
                bottom.setSz(BigInteger.valueOf(4));
                bottom.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder left = borders.addNewLeft();
                left.setColor(ReportColor.LINE.hex());
                left.setSz(BigInteger.valueOf(4));
                left.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder right = borders.addNewRight();
                right.setColor(ReportColor.LINE.hex());
                right.setSz(BigInteger.valueOf(4));
                right.setVal(STBorder.Enum.forString("single"));
            }
        }
    }

    private int @NotNull [] evenWidths(final int columns) {
        final int @NotNull [] widths = new int[columns];
        Arrays.fill(widths, 100 / columns);

        for (int i = 0; i < 100 % columns; i++) widths[i]++;

        return widths;
    }

    private void setTableWidths(final @NotNull XWPFTable table, final int... percents) {
        final @NotNull XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < row.getTableCells().size() && i < percents.length; i++) {
            propertiesOf(row.getCell(i)).addNewTcW().setW(percents[i] * 100);
            propertiesOf(row.getCell(i)).getTcW().setType(STTblWidth.Enum.forString("pct"));
        }
    }

    private void addFooter(final @NotNull XWPFDocument document) {
        final @NotNull XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        final @NotNull XWPFParagraph paragraph = footer.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        styledTextRun(paragraph.createRun(), ReportText.footerLead(), ReportFont.CAPTION, ReportColor.MUTED.hex());

        final @NotNull XWPFHyperlinkRun link = paragraph.createHyperlinkRun(ReportText.PLUGIN_URL);
        styledTextRun(link, ReportText.PLUGIN_NAME, ReportFont.CAPTION, ReportColor.LINK.hex());
        link.setUnderline(UnderlinePatterns.SINGLE);

        styledTextRun(paragraph.createRun(), Bundle.message("report.footer.suffix"), ReportFont.CAPTION, ReportColor.MUTED.hex());
    }

    private void styledTextRun(final @NotNull XWPFRun textRun, final @NotNull String text, final @NotNull ReportFont font, final @NotNull String color) {
        textRun.setText(text);
        textRun.setFontSize(font.roundedPoints());
        textRun.setFontFamily(ReportFont.FAMILY);
        textRun.setColor(color);
    }

    private void applyPageMargins(final @NotNull XWPFDocument document) {
        final @NotNull CTBody body = document.getDocument().getBody();
        final @NotNull CTSectPr sectionProperties = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        final @NotNull CTPageMar pageMargins = sectionProperties.isSetPgMar() ? sectionProperties.getPgMar() : sectionProperties.addNewPgMar();

        final long sideTwips = 720L;
        final long endTwips = 1440L;

        pageMargins.setLeft(sideTwips);
        pageMargins.setRight(sideTwips);
        pageMargins.setTop(endTwips);
        pageMargins.setBottom(endTwips);
    }
}
