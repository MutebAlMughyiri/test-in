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
import com.intellij.openapi.util.text.StringUtil;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
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
import org.testin.notifications.Notifier;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.ReportFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TestRunPdfGenerator {
    private final @NotNull Set<String> leftOut = new LinkedHashSet<>();
    private final @NotNull DeviceRgb DARK_NAVY = rgb(ReportColor.HEADING.hex());
    private final @NotNull DeviceRgb MEDIUM_BLUE = rgb(ReportColor.ACCENT.hex());
    private final @NotNull DeviceRgb DARK_GRAY = rgb(ReportColor.MUTED.hex());
    private final @NotNull DeviceRgb LIGHT_BG = rgb(ReportColor.PANEL.hex());
    private final @NotNull DeviceRgb BORDER_GRAY = rgb(ReportColor.LINE.hex());
    private final @NotNull DeviceRgb WHITE = rgb(ReportColor.PAGE.hex());
    private final @NotNull DeviceRgb BLACK = rgb(ReportColor.INK.hex());
    private final @NotNull DeviceRgb LINK_BLUE = rgb(ReportColor.LINK.hex());
    private @NotNull Optional<PdfFont> printsWith = Optional.empty();

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public byte @NotNull [] generate(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document document = new Document(pdf, pdf.getDefaultPageSize(), false)) {
            PdfFont boldFont = PdfFontFactory.createFont(ReportFont.PDF_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(ReportFont.PDF_REGULAR);

            // Rule-REPORT-018
            printsWith = Optional.of(regularFont);
            PdfFont italicFont = PdfFontFactory.createFont(ReportFont.PDF_ITALIC);

            final @NotNull String projectName = Services.getInstance(project, BoundTestProject.class).name();

            document.add(para(Bundle.message("report.title"))
                    .setFont(boldFont).setFontSize(ReportFont.TITLE.pt()).setFontColor(DARK_NAVY)
                    .setMarginBottom(2));

            document.add(para(ReportText.joined("  |  ", projectName, ReportText.joined(", ", TestRunConfiguration.PLATFORM.valueIn(testRunNode.getMarker()), TestRunConfiguration.COMPONENT.valueIn(testRunNode.getMarker()))))
                    .setFont(regularFont).setFontSize(ReportFont.SUBTITLE.pt()).setFontColor(MEDIUM_BLUE)
                    .setMarginBottom(0));

            document.add(para(testRunNode.getName())
                    .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(MEDIUM_BLUE)
                    .setPaddingBottom(4)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 2f))
                    .setMarginBottom(1));

            document.add(para(Bundle.message("report.confidential"))
                    .setFont(italicFont).setFontSize(ReportFont.CAPTION.pt()).setFontColor(DARK_GRAY)
                    .setMarginBottom(20));

            Paragraph sec1 = para(Bundle.message("report.heading.overview"))
                    .setFont(boldFont)
                    .setFontSize(ReportFont.SECTION.pt())
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(12);
            document.add(sec1);

            final @NotNull TestRunSummary summary = TestRunSummary.of(runItems.getAll());

            Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            for (final DetailRow row : ReportOverview.rowsFor(projectName, testRunNode, summary)) {
                addOverviewRow(overviewTable, row.caption(), row.value(), boldFont, regularFont);
            }

            document.add(overviewTable);

            Paragraph sec2 = para(Bundle.message("report.heading.execution"))
                    .setFont(boldFont)
                    .setFontSize(ReportFont.SECTION.pt())
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(9)
                    .setMarginTop(20);
            document.add(sec2);

            document.add(para(
                    Bundle.message("report.summary.named", testRunNode.getName(),
                            String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"))
                    .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(BLACK)
                    .setMarginBottom(12));

            final @NotNull List<ReportTile> tiles = ReportTile.shownFor(summary);

            final float[] tileWidths = new float[tiles.size()];
            Arrays.fill(tileWidths, 100f / tileWidths.length);

            Table statsTable = new Table(UnitValue.createPercentArray(tileWidths))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            for (final ReportTile tile : tiles) {
                addStatCell(statsTable, tile.valueIn(summary), tile.getLabel(), rgb(tile.getHex()), boldFont);
            }

            document.add(statsTable);

            final boolean analyzed = TestRunResultAnalysis.anyWrittenIn(testRunNode.getMarker().getResultAnalysis());

            if (analyzed) addAnalysis(document, testRunNode, summary, boldFont, regularFont);

            int sectionNumber = analyzed ? 4 : 3;
            for (final ReportSection section : ReportSection.values()) {
                final long count = section.count(summary);
                if (count == 0) continue;

                buildTestCaseTable(document, String.valueOf(sectionNumber++), section, section.description(String.valueOf(count)), runItems, boldFont, regularFont);
            }

            float pageWidth = pdf.getDefaultPageSize().getWidth();
            float leftMargin = document.getLeftMargin();
            float rightMargin = document.getRightMargin();

            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                final @NotNull Canvas footerCanvas = new Canvas(pdf.getPage(page),
                        new Rectangle(leftMargin, 0, pageWidth - leftMargin - rightMargin, 28));

                final @NotNull PdfCanvas pdfCanvas = footerCanvas.getPdfCanvas();
                pdfCanvas.setStrokeColor(BORDER_GRAY);
                pdfCanvas.setLineWidth(1.0f);
                pdfCanvas.moveTo(leftMargin, 34);
                pdfCanvas.lineTo(pageWidth - rightMargin, 34);
                pdfCanvas.stroke();

                footerCanvas.add(para()
                        .setFont(regularFont).setFontSize(ReportFont.CAPTION.pt()).setFontColor(DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .add(text(ReportText.footerLead()))
                        .add(new Link(ReportText.PLUGIN_NAME, PdfAction.createURI(ReportText.PLUGIN_URL))
                                .setFontColor(LINK_BLUE))
                        .add(text(Bundle.message("report.footer.suffix"))));

                footerCanvas.close();
            }

            document.close();
            sayWhatWasLeftOut(project);
            return baos.toByteArray();

        } catch (final IOException ex) {
            TestinLogger.error("PDF generation failed: " + FailureText.of(ex));
            throw new UncheckedIOException(ex);
        }
    }

    // UC-REPORT-001
    private void addAnalysis(final @NotNull Document document, final @NotNull TestRunNode testRunNode, final @NotNull TestRunSummary summary, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        document.add(para(Bundle.message("report.heading.analysis"))
                .setFont(boldFont)
                .setFontSize(ReportFont.SECTION.pt())
                .setFontColor(DARK_NAVY)
                .setPaddingBottom(3)
                .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                .setMarginBottom(9)
                .setMarginTop(20));

        for (final TestRunResultAnalysis section : TestRunResultAnalysis.values()) {
            final @NotNull String written = section.writtenIn(testRunNode.getMarker().getResultAnalysis());
            if (written.isEmpty()) continue;

            document.add(para(section.heading(summary))
                    .setFont(boldFont).setFontSize(ReportFont.LEAD.pt())
                    .setFontColor(rgb(section.getHexColor()))
                    .setMarginBottom(2));

            document.add(para(written)
                    .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)
                    .setMarginBottom(8));
        }
    }

    private @NotNull DeviceRgb rgb(final @NotNull String hex) {
        return new DeviceRgb(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16));
    }

    // UC-REPORT-001, Rule-REPORT-024
    private void buildTestCaseTable(final @NotNull Document document, final @NotNull String sectionNumber, final @NotNull ReportSection section, final @NotNull String description, final @NotNull RunItems runItems, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        document.add(para(sectionNumber + ". " + section.getTitle())
                .setFont(boldFont)
                .setFontSize(ReportFont.SECTION.pt())
                .setFontColor(DARK_NAVY)
                .setPaddingBottom(3)
                .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                .setMarginBottom(9)
                .setMarginTop(20));

        document.add(para(description)
                .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(BLACK)
                .setMarginBottom(12));

        final @NotNull List<TestRunEditorAttributes> failureDetail = section.getFailureDetailColumns();
        Table table = new Table(2 + failureDetail.size())
                .useAllAvailableWidth()
                .setAutoLayout()
                .setBorder(Border.NO_BORDER);

        final @NotNull DeviceRgb headerBg = rgb(section.getHexColor());
        final @NotNull DeviceRgb headerFg = rgb(section.textHex());
        addTestCaseTableHeader(table, "#", headerBg, headerFg, boldFont);
        addTestCaseTableHeader(table, Bundle.message("caption.test.case"), headerBg, headerFg, boldFont);
        for (final TestRunEditorAttributes detail : failureDetail) {
            addTestCaseTableHeader(table, detail.getName(), headerBg, headerFg, boldFont);
        }

        int idx = 1;
        boolean alt = true;
        for (RunItem runItem : runItems.getAll()) {
            if (!section.matches(runItem)) continue;
            ProgressManager.checkCanceled();

            DeviceRgb rowBg = alt ? LIGHT_BG : WHITE;
            alt = !alt;

            table.addCell(new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                    .add(para(String.valueOf(idx))
                            .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER)));

            final @NotNull String testCaseName = runItem.shownTestCase().getDescription();
            final @NotNull String shownTestCaseName = testCaseName.isEmpty() ? "—" : testCaseName;
            final @NotNull Cell testCaseCell = new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6);
            testCaseCell.add(para(shownTestCaseName)
                    .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)
                    .setMarginBottom(0));

            // Rule-REPORT-027
            ReportText.otherExecutions(runItem).ifPresent(others -> testCaseCell.add(para(others).setFont(regularFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY)));
            table.addCell(testCaseCell);

            if (section.isWithFailureDetail())
                addFailureDetail(table, testCaseCell, runItem, rowBg, boldFont, regularFont);

            idx++;
        }

        document.add(table);
    }

    private void addFailureDetail(final @NotNull Table table, final @NotNull Cell testCaseCell, final @NotNull RunItem runItem, final @NotNull DeviceRgb rowBg, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        final @NotNull String actualResult = runItem.getActualResult();
        final @NotNull Paragraph actual = para(Bundle.message("report.actual.result", actualResult.isEmpty() ? "—" : actualResult))
                .setFont(regularFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY);

        runItem.bugIssue().ifPresent(url -> actual.add(text(" ("))
                .add(new Link(BugIssueUrl.shortReference(url), PdfAction.createURI(url)).setFontColor(LINK_BLUE))
                .add(text(")")));
        testCaseCell.add(actual);

        final @NotNull BugPriority priority = runItem.getBugPriority();
        table.addCell(runItemStatusCell(priority.getLabel(), rgb(priority.getEmphasis().getHexColor()), rowBg, boldFont));

        final @NotNull BugSeverity severity = runItem.getBugSeverity();
        table.addCell(runItemStatusCell(severity.getLabel(), rgb(severity.getEmphasis().getHexColor()), rowBg, boldFont));
    }

    private @NotNull Cell runItemStatusCell(final @NotNull String text, final @NotNull DeviceRgb color, final @NotNull DeviceRgb rowBg, final @NotNull PdfFont boldFont) {
        return new Cell()
                .setBackgroundColor(rowBg)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(para(text)
                        .setFont(boldFont).setFontSize(ReportFont.BODY.pt()).setFontColor(color)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private void addTestCaseTableHeader(final @NotNull Table table, final @NotNull String text, final @NotNull DeviceRgb bgColor, final @NotNull DeviceRgb textColor, final @NotNull PdfFont boldFont) {
        table.addCell(new Cell()
                .setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(6).setPaddingRight(6)
                .add(para(text)
                        .setFont(boldFont).setFontSize(ReportFont.HEADING.pt()).setFontColor(textColor)
                        .setTextAlignment(TextAlignment.CENTER)));
    }

    private void addOverviewRow(final @NotNull Table table, final @NotNull String label, final @NotNull String value, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        table.addCell(new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(para(label)
                        .setFont(boldFont).setFontSize(ReportFont.HEADING.pt()).setFontColor(DARK_NAVY)));

        table.addCell(new Cell()
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(para(value)
                        .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)));
    }

    private void addStatCell(final @NotNull Table table, final @NotNull String number, final @NotNull String label, final @NotNull DeviceRgb numberColor, final @NotNull PdfFont boldFont) {
        Cell cell = new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6);

        cell.add(para(number)
                .setFont(boldFont).setFontSize(ReportFont.FIGURE.pt()).setFontColor(numberColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        cell.add(para(label)
                .setFont(boldFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        table.addCell(cell);
    }

    private @NotNull Paragraph para(final @NotNull String text) {
        return new Paragraph(printable(text));
    }

    private @NotNull Paragraph para() {
        return new Paragraph();
    }

    private @NotNull Text text(final @NotNull String text) {
        return new Text(printable(text));
    }

    // UC-REPORT-001, Rule-REPORT-018
    private @NotNull String printable(final @NotNull String text) {
        printsWith.ifPresent(font -> {
            final boolean loses = text.codePoints()
                    .filter(cp -> !Character.isWhitespace(cp) && !Character.isISOControl(cp))
                    .anyMatch(cp -> !font.containsGlyph(cp));
            if (loses) leftOut.add(text);
        });
        return text;
    }

    // UC-REPORT-001, Rule-REPORT-018
    private void sayWhatWasLeftOut(final @NotNull Project project) {
        if (leftOut.isEmpty()) return;

        final @NotNull String howMuch = leftOut.size() == 1
                ? Bundle.message("report.pdf.left.out.one")
                : Bundle.message("report.pdf.left.out.many", String.valueOf(leftOut.size()));
        final @NotNull String example = StringUtil.shortenTextWithEllipsis(leftOut.iterator().next().strip(), 60, 0);

        TestinLogger.warn("The PDF left characters out of " + leftOut.size() + " text(s) its font cannot print");
        Services.getInstance(project, Notifier.class).warn(project, Bundle.message("report.pdf.left.out.title"),
                Bundle.message("report.pdf.left.out.message", howMuch, example));
    }
}
