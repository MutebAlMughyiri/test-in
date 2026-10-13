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

import com.intellij.notification.Notification;
import com.intellij.openapi.application.WriteAction;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.importexport.FileFormat;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Bundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportFormatsIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TEST_PROJECT = "NAFATH";

    private static final @NotNull String TEST_RUN = "Cycle-1";

    private static final @NotNull String STACKTRACE = "java.lang.AssertionError: the account stayed open";

    private static final @NotNull String SCREENSHOT = "screenshot-1.png";

    private final @NotNull UUID opens = UUID.randomUUID();

    private final @NotNull UUID locks = UUID.randomUUID();

    private final @NotNull UUID deleted = UUID.randomUUID();

    private String wasBound;

    private @NotNull TestProjectNode testProject = new TestProjectNode();

    private @NotNull TestSetNode login = new TestSetNode();

    private static @NotNull String withoutTags(final @NotNull String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private static @NotNull String squeezed(final @NotNull String text) {
        return text.replaceAll("\\s", "");
    }

    private static void assertPrints(final @NotNull String format, final @NotNull String report, final @NotNull String expected) {
        assertTrue(format + " does not print: " + expected, squeezed(report).contains(squeezed(expected)));
    }

    private static void assertLeavesOut(final @NotNull String format, final @NotNull String report, final @NotNull String unexpected) {
        assertFalse(format + " prints what it should leave out: " + unexpected, report.contains(unexpected));
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private @NotNull NodeMapper mapper() {
        return Services.getInstance(getProject(), NodeMapper.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        wasBound = bound().name();
        bound().choose(TEST_PROJECT);

        WriteAction.runAndWait(() -> {
            testProject = mapper().setTestProjectNode(root.resolve(TEST_PROJECT));
            nodes().addTestProject(testProject);

            login = mapper().getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes().addTestSet(login);
        });

        aTestCase(opens, "Open the app\nLog in with a valid user");
        aTestCase(locks, "Lock the account after three wrong passwords");
    }

    @Override
    protected void tearDown() {
        bound().choose(wasBound);
        super.tearDown();
    }

    private void aTestCase(final @NotNull UUID testCaseId, final @NotNull String description) {
        final @NotNull TestCase testCase = TestCase.builder().id(testCaseId).description(description).order(testCaseId.toString()).build();
        testCase.setParent(login);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(login.getPath(), testCase);
    }

    private @NotNull TestRunNode aTestRun(final @NotNull String named, final @NotNull List<RunItem> runItems) {
        final @NotNull TestRunNode testRun = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode created = mapper().setTestRunNode(testProject.getTestRunsFolder().getPath().resolve(named), testProject.getTestRunsFolder());
            nodes().addTestRunNode(created);
            return created;
        });

        Services.getInstance(getProject(), TestRuns.class).putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(runItems)));
        return testRun;
    }

    private @NotNull TestRunNode theTestRun() {
        return aTestRun(TEST_RUN, List.of(
                new RunItem().setId(opens).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                new RunItem().setId(locks).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The account\nstayed open").stacktrace(STACKTRACE).screenshots(List.of(SCREENSHOT)).build())),
                new RunItem().setId(deleted).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))));
    }

    private @NotNull RunItems resultsOf(final @NotNull TestRunNode testRun) {
        return Services.getInstance(getProject(), TestRuns.class).getRunItems(testRun.getPath());
    }

    private byte @NotNull [] report(final @NotNull FileFormat format, final @NotNull TestRunNode testRun) {
        return format.generateReport(getProject(), testRun, resultsOf(testRun));
    }

    private @NotNull String html(final @NotNull TestRunNode testRun) {
        return new String(report(FileFormat.HTML, testRun), StandardCharsets.UTF_8);
    }

    private @NotNull String pdf(final @NotNull TestRunNode testRun) {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(report(FileFormat.PDF, testRun))))) {
            final @NotNull StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++)
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(page))).append('\n');
            return text.toString();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the PDF back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull String word(final @NotNull TestRunNode testRun) {
        try (XWPFWordExtractor extractor = new XWPFWordExtractor(new XWPFDocument(new ByteArrayInputStream(report(FileFormat.WORD, testRun))))) {
            return extractor.getText();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the Word document back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull List<List<List<String>>> sheets(final @NotNull TestRunNode testRun) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report(FileFormat.XLSX, testRun)))) {
            final @NotNull DataFormatter formatter = new DataFormatter();
            final @NotNull List<List<List<String>>> sheets = new ArrayList<>();

            for (final Sheet sheet : workbook) {
                final @NotNull List<List<String>> rows = new ArrayList<>();
                for (final Row row : sheet) {
                    final @NotNull List<String> cells = new ArrayList<>();
                    for (final Cell cell : row) cells.add(formatter.formatCellValue(cell));
                    rows.add(cells);
                }
                sheets.add(rows);
            }
            return sheets;
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the spreadsheet back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull List<String> sheetNames(final @NotNull TestRunNode testRun) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report(FileFormat.XLSX, testRun)))) {
            final @NotNull List<String> names = new ArrayList<>();
            for (final Sheet sheet : workbook) names.add(sheet.getSheetName());
            return names;
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the spreadsheet back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull String excel(final @NotNull TestRunNode testRun) {
        final @NotNull StringBuilder text = new StringBuilder();
        sheets(testRun).forEach(sheet -> sheet.forEach(row -> text.append(String.join("\t", row)).append('\n')));
        return text.toString();
    }

    // Rule-REPORT-002
    public void testEveryFormatPrintsTheSameFigures() {
        final @NotNull TestRunNode testRun = theTestRun();
        final @NotNull TestRunSummary summary = TestRunSummary.of(resultsOf(testRun).getAll());
        final @NotNull String figures = Bundle.message("report.summary.named", TEST_RUN, String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%");

        assertPrints("The web page", withoutTags(html(testRun)), figures);
        assertPrints("The PDF", pdf(testRun), figures);
        assertPrints("The Word document", word(testRun), figures);
        assertPrints("The spreadsheet", excel(testRun), figures);
    }

    // Rule-REPORT-005
    public void testTheTestProjectNamedIsTheOneOnTheReport() {
        final @NotNull TestRunNode testRun = theTestRun();
        final @NotNull String codeProject = getProject().getName();

        for (final String report : List.of(html(testRun), pdf(testRun), word(testRun), excel(testRun))) {
            assertPrints("A report", report, TEST_PROJECT);
            assertLeavesOut("A report", report, codeProject);
        }
    }

    // Rule-REPORT-017
    public void testNoReportShowsAScreenshotAndOnlyTheWebPagePrintsTheError() {
        final @NotNull TestRunNode testRun = theTestRun();
        final @NotNull String html = html(testRun);

        assertPrints("The web page", html, STACKTRACE);
        assertLeavesOut("The web page", html, "<img");
        assertLeavesOut("The web page", html, SCREENSHOT);

        for (final String report : List.of(pdf(testRun), word(testRun), excel(testRun))) {
            assertLeavesOut("A report", report, STACKTRACE);
            assertLeavesOut("A report", report, SCREENSHOT);
        }
    }

    // Rule-REPORT-018
    public void testThePdfSaysWhatItCouldNotPrint() {
        aTestCase(opens, "سجل الدخول");
        final @NotNull TestRunNode testRun = aTestRun("Cycle-Arabic", List.of(new RunItem().setId(opens).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))));

        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        report(FileFormat.PDF, testRun);

        Await.until("the PDF left Arabic out and said nothing", () -> said.stream().anyMatch(notification -> notification.getTitle().equals(Bundle.message("report.pdf.left.out.title"))));
    }

    // Rule-REPORT-019
    public void testEveryFormatNamesTheColumnsTheSameWay() {
        final @NotNull TestRunNode testRun = theTestRun();
        final @NotNull List<String> words = List.of(Bundle.message("caption.test.case"), TestRunEditorFields.BUG_PRIORITY.getName(), TestRunEditorFields.BUG_SEVERITY.getName());

        for (final String column : words) {
            assertPrints("The web page", html(testRun), ">" + column + "</th>");
            assertPrints("The PDF", pdf(testRun), column);
            assertPrints("The Word document", word(testRun), column);
        }

        final @NotNull List<String> header = sheets(testRun).get(1).getFirst();
        for (final String column : words)
            assertTrue("The spreadsheet names a column differently: " + column + " is not in " + header, header.contains(column));
    }

    // Rule-REPORT-020
    public void testTheSpreadsheetHasAnOverviewSheetAndATestCasesSheet() {
        final @NotNull TestRunNode testRun = theTestRun();

        assertEquals(List.of(Bundle.message("report.excel.sheet.overview"), Bundle.message("report.excel.sheet.test.cases")), sheetNames(testRun));

        final @NotNull List<List<String>> testCases = sheets(testRun).get(1);
        assertEquals("the Test Cases sheet holds the column names and one row per test case, and nothing else",
                resultsOf(testRun).getAll().size() + 1, testCases.size());
    }

    // Rule-REPORT-021
    public void testADeletedTestCaseIsNamedAsTheTestRunNamesIt() {
        final @NotNull TestRunNode testRun = theTestRun();
        final @NotNull String named = Bundle.message("testcase.deleted", deleted);

        assertPrints("The web page", html(testRun), named);
        assertPrints("The PDF", pdf(testRun), named);
        assertPrints("The Word document", word(testRun), named);
        assertPrints("The spreadsheet", excel(testRun), named);
    }

    // Rule-REPORT-023
    public void testALineBreakTheTesterTypedStaysALineBreak() {
        final @NotNull TestRunNode testRun = theTestRun();

        for (final String report : List.of(pdf(testRun), word(testRun), excel(testRun))) {
            assertTrue("a description was run together onto one line", report.lines().anyMatch(line -> line.strip().endsWith("Open the app")));
            assertTrue("an actual result was run together onto one line", report.lines().anyMatch(line -> line.strip().startsWith("stayed open")));
        }

        final @NotNull String html = html(testRun);
        assertPrints("The web page", html, "Open the app\nLog in");
        assertTrue("the web page lays a test case's description out on one line", html.matches("(?s).*\\.detail-table td \\{[^}]*white-space: pre-wrap.*"));
        assertTrue("the web page lays an actual result out on one line", html.matches("(?s).*\\.actual \\{[^}]*white-space: pre-wrap.*"));
    }

    // Rule-REPORT-026
    public void testEveryFormatListsTheRunItemsInTheTestRunsOrder() {
        final @NotNull List<UUID> inTheSet = Services.getInstance(getProject(), TestCases.class).getTestCasesUnder(testProject.getTestCasesFolder()).stream().map(TestCase::getId).toList();
        final @NotNull UUID first = inTheSet.getFirst();
        final @NotNull UUID second = inTheSet.get(1);
        final @NotNull TestRunNode testRun = aTestRun("Stored backwards", List.of(
                new RunItem().setId(second).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                new RunItem().setId(first).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))));
        final @NotNull String firstWords = squeezed(first.equals(opens) ? "Open the app" : "Lock the account");
        final @NotNull String secondWords = squeezed(second.equals(opens) ? "Open the app" : "Lock the account");

        for (final List<String> report : List.of(
                List.of("The web page", withoutTags(html(testRun))),
                List.of("The PDF", pdf(testRun)),
                List.of("The Word document", word(testRun)),
                List.of("The spreadsheet", excel(testRun)))) {
            final @NotNull String text = squeezed(report.get(1));
            assertTrue(report.getFirst() + " does not list the run items in the test run's order",
                    text.contains(firstWords) && text.indexOf(firstWords) < text.indexOf(secondWords));
        }
    }
}
