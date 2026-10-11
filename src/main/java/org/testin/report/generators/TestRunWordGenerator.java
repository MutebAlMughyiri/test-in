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
import org.testin.logger.Logger;
import org.testin.model.ReportColor;
import org.testin.model.testrun.RunItems;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.markers.DetailRow;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.ReportFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public final class TestRunWordGenerator {
    final String NO_BORDER = "";

    final String DARK_NAVY = ReportColor.HEADING.hex();
    final String MEDIUM_BLUE = ReportColor.ACCENT.hex();
    final String DARK_GRAY = ReportColor.MUTED.hex();
    final String LINK_BLUE = ReportColor.LINK.hex();
    final String LIGHT_BG = ReportColor.PANEL.hex();
    final String BORDER_GRAY = ReportColor.LINE.hex();
    final String WHITE = ReportColor.PAGE.hex();
    final String BLACK = ReportColor.INK.hex();

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {
                final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

                addText(doc, Bundle.message("report.title"), ReportFont.TITLE.ptRounded(), true, DARK_NAVY, NO_BORDER, 2);

                addText(doc, ReportText.joined("  |  ", projectName, ReportText.joined(", ", TestRunConfiguration.PLATFORM.valueIn(testRunNode.getMarker()), TestRunConfiguration.COMPONENT.valueIn(testRunNode.getMarker()))),
                        ReportFont.SUBTITLE.ptRounded(), false, MEDIUM_BLUE, NO_BORDER, 0);
                addText(doc, testRunNode.getName(), ReportFont.LEAD.ptRounded(), false, MEDIUM_BLUE, DARK_NAVY, 1);

                XWPFParagraph conf = addText(doc, Bundle.message("report.confidential"), ReportFont.CAPTION.ptRounded(), false, DARK_GRAY, NO_BORDER, 20);
                setItalic(conf);

                addHeading(doc, Bundle.message("report.heading.overview"), 0, 15);

                final @NotNull TestRunSummary summary = TestRunSummary.of(runItems.getAll());

                XWPFTable overviewTable = doc.createTable(1, 2);
                overviewTable.setWidth("100%");
                overviewTable.setWidthType(TableWidthType.PCT);
                setTableWidths(overviewTable, 30, 70);

                int overviewRow = 0;
                for (final DetailRow row : ReportOverview.rowsFor(projectName, testRunNode, summary)) {
                    addOverviewRow(overviewTable, overviewRow++, row.caption(), row.value());
                }

                setTableBorders(overviewTable);

                addHeading(doc, Bundle.message("report.heading.execution"), 20, 12);

                addText(doc, Bundle.message("report.summary.named", testRunNode.getName(),
                                String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"),
                        ReportFont.LEAD.ptRounded(), false, BLACK, NO_BORDER, 12);

                final @NotNull List<ReportTile> headline = ReportTile.shownFor(summary);
                final int tiles = headline.size();

                XWPFTable statsTable = doc.createTable(1, tiles);
                statsTable.setWidth("100%");
                statsTable.setWidthType(TableWidthType.PCT);
                setTableBorders(statsTable);
                setTableWidths(statsTable, evenWidths(tiles));

                for (int tile = 0; tile < tiles; tile++) {
                    final @NotNull ReportTile figure = headline.get(tile);
                    addStatCell(statsTable, tile, figure.valueIn(summary), figure.getLabel(), figure.getHex());
                }

                final boolean analyzed = TestRunResultAnalysis.anyWrittenIn(testRunNode.getMarker().getResultAnalysis());

                if (analyzed) {
                    addHeading(doc, Bundle.message("report.heading.analysis"), 20, 12);

                    for (final TestRunResultAnalysis section : TestRunResultAnalysis.values()) {
                        final @NotNull String written = section.writtenIn(testRunNode.getMarker().getResultAnalysis());
                        if (written.isEmpty()) continue;

                        addColoredCount(doc, section.heading(summary), section.getHexColor());
                        addText(doc, written, ReportFont.BODY.ptRounded(), false, BLACK, NO_BORDER, 8);
                    }
                }

                int sectionNumber = analyzed ? 4 : 3;
                for (final ReportSection section : ReportSection.values()) {
                    final long count = section.count(summary);
                    if (count == 0) continue;

                    buildTestCaseTable(doc, String.valueOf(sectionNumber++), section, section.description(String.valueOf(count)), runItems);
                }

                addFooter(doc);

                applyPageMargins(doc);

                doc.write(baos);
            }
            return baos.toByteArray();

        } catch (final IOException ex) {
            Logger.error("Word generation failed: " + FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }

    private @NotNull XWPFParagraph addText(final @NotNull XWPFDocument doc, final @NotNull String text, final int size, final boolean bold, final @NotNull String color, final @NotNull String bottomBorder, final int spacingAfterPt) {
        final @NotNull XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(spacingAfterPt * 20);
        final @NotNull XWPFRun run = p.createRun();
        writeLines(run, text, false);
        run.setFontSize(size);
        run.setFontFamily(ReportFont.FAMILY);
        run.setBold(bold);
        run.setColor(color);
        if (!bottomBorder.isEmpty()) {
            final @NotNull CTBorder bottom = p.getCTPPr().addNewPBdr().addNewBottom();
            bottom.setVal(STBorder.Enum.forString("single"));
            bottom.setSz(BigInteger.valueOf(16));
            bottom.setColor(bottomBorder);
        }
        return p;
    }

    private void setItalic(final @NotNull XWPFParagraph p) {
        for (final XWPFRun run : p.getRuns()) {
            run.setItalic(true);
        }
    }

    private void addHeading(final @NotNull XWPFDocument doc, final @NotNull String text, final int beforePt, final int afterPt) {
        final @NotNull XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(beforePt * 20);
        p.setSpacingAfter(afterPt * 20);
        final @NotNull XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(ReportFont.SECTION.ptRounded());
        run.setFontFamily(ReportFont.FAMILY);
        run.setBold(true);
        run.setColor(DARK_NAVY);
        final @NotNull CTBorder headingBottom = p.getCTPPr().addNewPBdr().addNewBottom();
        headingBottom.setVal(STBorder.Enum.forString("single"));
        headingBottom.setSz(BigInteger.valueOf(8));
        headingBottom.setColor(DARK_NAVY);
    }

    private void addOverviewRow(final @NotNull XWPFTable table, final int rowIdx, final @NotNull String label, final @NotNull String value) {
        final @NotNull XWPFTableRow row = rowIdx == 0 ? table.getRow(0) : table.createRow();
        final @NotNull XWPFTableCell labelCell = row.getCell(0);
        final @NotNull XWPFTableCell valueCell = row.getCell(1);
        shadeCell(labelCell, LIGHT_BG);
        setCellPadding(labelCell, 4, 8, 4, 8);
        setCellPadding(valueCell, 4, 8, 4, 8);
        setCellText(labelCell, label, ReportFont.HEADING.ptRounded(), true, DARK_NAVY);
        setCellText(valueCell, value, ReportFont.BODY.ptRounded(), false, BLACK);
    }

    private void addStatCell(final @NotNull XWPFTable table, final int col, final @NotNull String number, final @NotNull String label, final @NotNull String numberColor) {
        final @NotNull XWPFTableRow row = table.getRow(0);
        final @NotNull XWPFTableCell cell = row.getCell(col);
        shadeCell(cell, LIGHT_BG);
        setCellPadding(cell, 8, 6, 8, 6);
        setCellText(cell, number, ReportFont.FIGURE.ptRounded(), true, numberColor);

        cell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

        final @NotNull XWPFParagraph lp = cell.addParagraph();
        lp.setAlignment(ParagraphAlignment.CENTER);
        lp.setSpacingBefore(80);
        final @NotNull XWPFRun lrun = lp.createRun();
        lrun.setText(label);
        lrun.setFontSize(ReportFont.SMALL.ptRounded());
        lrun.setFontFamily(ReportFont.FAMILY);
        lrun.setBold(true);
        lrun.setColor(DARK_GRAY);
    }

    private void addColoredCount(final @NotNull XWPFDocument doc, final @NotNull String heading, final @NotNull String headingColor) {
        final @NotNull XWPFParagraph hp = doc.createParagraph();
        hp.setSpacingAfter(120);

        final @NotNull XWPFRun hrun = hp.createRun();
        hrun.setText(heading);
        hrun.setFontSize(ReportFont.LEAD.ptRounded());
        hrun.setFontFamily(ReportFont.FAMILY);
        hrun.setBold(true);
        hrun.setColor(headingColor);
    }

    private void buildTestCaseTable(final @NotNull XWPFDocument doc, final @NotNull String sectionNumber, final @NotNull ReportSection section, final @NotNull String description, final @NotNull RunItems runItems) {
        addHeading(doc, sectionNumber + ". " + section.getTitle(), 20, 12);
        addText(doc, description, ReportFont.LEAD.ptRounded(), false, BLACK, NO_BORDER, 12);

        final @NotNull List<TestRunEditorAttributes> failureDetail = section.getFailureDetailColumns();
        XWPFTable table = doc.createTable(1, 2 + failureDetail.size());
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        final @NotNull String headerBg = section.getHexColor();
        final @NotNull String headerFg = section.textHex();
        XWPFTableRow headerRow = table.getRow(0);
        addTestCaseHeader(headerRow, 0, "#", headerBg, headerFg);
        addTestCaseHeader(headerRow, 1, Bundle.message("caption.test.case"), headerBg, headerFg);
        int column = 2;
        for (final TestRunEditorAttributes detail : failureDetail) {
            addTestCaseHeader(headerRow, column++, detail.getName(), headerBg, headerFg);
        }

        int idx = 1;
        boolean alt = true;
        for (RunItem runItem : runItems.getAll()) {
            if (!section.matches(runItem)) continue;
            ProgressManager.checkCanceled();

            String rowBg = alt ? LIGHT_BG : WHITE;
            alt = !alt;

            XWPFTableRow row = table.createRow();
            XWPFTableCell numCell = row.getCell(0);
            shadeCell(numCell, rowBg);
            setCellPadding(numCell, 4, 6, 4, 6);
            setCellText(numCell, String.valueOf(idx), ReportFont.BODY.ptRounded(), false, DARK_GRAY);
            numCell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

            XWPFTableCell tcCell = row.getCell(1);
            shadeCell(tcCell, rowBg);
            setCellPadding(tcCell, 4, 6, 4, 6);
            final @NotNull String testCaseName = runItem.shownTestCase().getDescription();
            final @NotNull String tcName = testCaseName.isEmpty() ? "—" : testCaseName;
            setCellText(tcCell, tcName, ReportFont.BODY.ptRounded(), false, BLACK);

            // Rule-REPORT-027
            final @NotNull String others = ReportText.otherExecutions(runItem);
            if (!others.isEmpty()) styledRun(tcCell.addParagraph().createRun(), others, ReportFont.SMALL, DARK_GRAY);

            if (section.isWithFailureDetail()) addFailureDetail(row, runItem, rowBg);

            idx++;
        }

        setTableBorders(table);
        autoFitToContent(table);
    }

    private void addFailureDetail(final @NotNull XWPFTableRow row, final @NotNull RunItem runItem, final @NotNull String rowBg) {
        final @NotNull String actualResult = runItem.getActualResult();
        final @NotNull XWPFParagraph ap = row.getCell(1).addParagraph();
        styledRun(ap.createRun(), Bundle.message("report.actual.result", actualResult.isEmpty() ? "—" : actualResult), ReportFont.SMALL, DARK_GRAY);

        runItem.bugIssue().ifPresent(url -> {
            styledRun(ap.createRun(), " (", ReportFont.SMALL, DARK_GRAY);
            final @NotNull XWPFHyperlinkRun issue = ap.createHyperlinkRun(url);
            styledRun(issue, BugIssueUrl.shortReference(url), ReportFont.SMALL, LINK_BLUE);
            issue.setUnderline(UnderlinePatterns.SINGLE);
            styledRun(ap.createRun(), ")", ReportFont.SMALL, DARK_GRAY);
        });

        final @NotNull XWPFTableCell priCell = row.getCell(2);
        shadeCell(priCell, rowBg);
        setCellPadding(priCell, 4, 6, 4, 6);
        final @NotNull BugPriority pri = runItem.getBugPriority();
        setCellText(priCell, pri.getLabel(), ReportFont.BODY.ptRounded(), true, pri.getEmphasis().getHexColor());

        final @NotNull XWPFTableCell sevCell = row.getCell(3);
        shadeCell(sevCell, rowBg);
        setCellPadding(sevCell, 4, 6, 4, 6);
        final @NotNull BugSeverity sev = runItem.getBugSeverity();
        setCellText(sevCell, sev.getLabel(), ReportFont.BODY.ptRounded(), true, sev.getEmphasis().getHexColor());
    }

    private void autoFitToContent(final @NotNull XWPFTable table) {
        final @NotNull CTTblPr properties = table.getCTTbl().getTblPr();
        final @NotNull CTTblLayoutType layout =
                properties.isSetTblLayout() ? properties.getTblLayout() : properties.addNewTblLayout();
        layout.setType(STTblLayoutType.AUTOFIT);

        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcPr cellProperties = getTcPr(cell);
                (cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW())
                        .setType(STTblWidth.AUTO);
            }
        }
    }

    private void addTestCaseHeader(final @NotNull XWPFTableRow headerRow, final int col, final @NotNull String text, final @NotNull String bgColor, final @NotNull String textColor) {
        final @NotNull XWPFTableCell cell = headerRow.getCell(col);
        shadeCell(cell, bgColor);
        setCellPadding(cell, 5, 6, 5, 6);
        setCellText(cell, text, ReportFont.HEADING.ptRounded(), true, textColor);
    }

    // UC-REPORT-001, Rule-REPORT-023
    private void writeLines(final @NotNull XWPFRun run, final @NotNull String text, final boolean replaceFirst) {
        final @NotNull List<String> lines = text.lines().toList();

        if (lines.isEmpty()) {
            run.setText("", 0);
            return;
        }

        if (replaceFirst) run.setText(lines.getFirst(), 0);
        else run.setText(lines.getFirst());

        for (final String line : lines.subList(1, lines.size())) {
            run.addBreak();
            run.setText(line);
        }
    }

    private void setCellText(final @NotNull XWPFTableCell cell, final @NotNull String text, final int size, final boolean bold, final @NotNull String color) {
        final @NotNull XWPFParagraph p = cell.getParagraphs().getFirst();
        if (p.getRuns().isEmpty()) {
            final @NotNull XWPFRun run = p.createRun();
            writeLines(run, text, false);
            run.setFontSize(size);
            run.setFontFamily(ReportFont.FAMILY);
            run.setBold(bold);
            run.setColor(color);

        } else {
            final @NotNull XWPFRun run = p.getRuns().getFirst();
            writeLines(run, text, true);
            run.setFontSize(size);
            run.setFontFamily(ReportFont.FAMILY);
            run.setBold(bold);
            run.setColor(color);
        }
    }

    private void shadeCell(final @NotNull XWPFTableCell cell, final @NotNull String hex) {
        getTcPr(cell).addNewShd().setFill(hex);
    }

    private void setCellPadding(final @NotNull XWPFTableCell cell, final int topPt, final int leftPt, final int bottomPt, final int rightPt) {
        final @NotNull CTTcMar mar = getTcPr(cell).addNewTcMar();
        final @NotNull CTTblWidth top = mar.addNewTop();
        top.setW(topPt * 20);
        top.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth left = mar.addNewLeft();
        left.setW(leftPt * 20);
        left.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth bottom = mar.addNewBottom();
        bottom.setW(bottomPt * 20);
        bottom.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth right = mar.addNewRight();
        right.setW(rightPt * 20);
        right.setType(STTblWidth.Enum.forString("dxa"));
    }

    private @NotNull CTTcPr getTcPr(final @NotNull XWPFTableCell cell) {
        final @NotNull CTTc ct = cell.getCTTc();
        return ct.isSetTcPr() ? ct.getTcPr() : ct.addNewTcPr();
    }

    private void setTableBorders(final @NotNull XWPFTable table) {
        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcBorders borders = getTcPr(cell).addNewTcBorders();
                final @NotNull CTBorder top = borders.addNewTop();
                top.setColor(BORDER_GRAY);
                top.setSz(BigInteger.valueOf(4));
                top.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder bottom = borders.addNewBottom();
                bottom.setColor(BORDER_GRAY);
                bottom.setSz(BigInteger.valueOf(4));
                bottom.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder left = borders.addNewLeft();
                left.setColor(BORDER_GRAY);
                left.setSz(BigInteger.valueOf(4));
                left.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder right = borders.addNewRight();
                right.setColor(BORDER_GRAY);
                right.setSz(BigInteger.valueOf(4));
                right.setVal(STBorder.Enum.forString("single"));
            }
        }
    }

    private int @NotNull [] evenWidths(final int columns) {
        final int[] widths = new int[columns];
        Arrays.fill(widths, 100 / columns);

        for (int i = 0; i < 100 % columns; i++) widths[i]++;

        return widths;
    }

    private void setTableWidths(final @NotNull XWPFTable table, final int... percents) {
        final @NotNull XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < row.getTableCells().size() && i < percents.length; i++) {
            getTcPr(row.getCell(i)).addNewTcW().setW(percents[i] * 100);
            getTcPr(row.getCell(i)).getTcW().setType(STTblWidth.Enum.forString("pct"));
        }
    }

    private void addFooter(final @NotNull XWPFDocument doc) {
        final @NotNull XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        final @NotNull XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        styledRun(p.createRun(), ReportText.footerLead(), ReportFont.CAPTION, DARK_GRAY);

        final @NotNull XWPFHyperlinkRun link = p.createHyperlinkRun(ReportText.PLUGIN_URL);
        styledRun(link, ReportText.PLUGIN_NAME, ReportFont.CAPTION, LINK_BLUE);
        link.setUnderline(UnderlinePatterns.SINGLE);

        styledRun(p.createRun(), Bundle.message("report.footer.suffix"), ReportFont.CAPTION, DARK_GRAY);
    }

    private void styledRun(final @NotNull XWPFRun run, final @NotNull String text, final @NotNull ReportFont font, final @NotNull String color) {
        run.setText(text);
        run.setFontSize(font.ptRounded());
        run.setFontFamily(ReportFont.FAMILY);
        run.setColor(color);
    }

    private void applyPageMargins(final @NotNull XWPFDocument doc) {
        final @NotNull CTBody body = doc.getDocument().getBody();
        final @NotNull CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        final @NotNull CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();

        final long sideTwips = 720L;
        final long endTwips = 1440L;

        pgMar.setLeft(sideTwips);
        pgMar.setRight(sideTwips);
        pgMar.setTop(endTwips);
        pgMar.setBottom(endTwips);
    }
}
