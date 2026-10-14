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

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.importexport.FileFormat;
import org.testin.importexport.exports.ExportNotice;
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
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class ReportDeliveryIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TEST_PROJECT = "NAFATH";

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();
    private final @NotNull UUID opens = UUID.randomUUID();
    private final @NotNull UUID locks = UUID.randomUUID();
    private @NotNull List<Notification> said = List.of();
    private String wasBound;

    private Path testin;

    private @NotNull TestProjectNode testProject = new TestProjectNode();

    private @NotNull TestSetNode login = new TestSetNode();

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull AnAction theLink(final @NotNull Notification message, final @NotNull String named) {
        return message.getActions().stream()
                .filter(action -> named.equals(action.getTemplatePresentation().getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the message has no " + named + " link: " + message.getActions().stream().map(offeredAction -> offeredAction.getTemplatePresentation().getText()).toList()));
    }

    private static @NotNull List<Path> everythingUnder(final @NotNull Path folder) {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.sorted(Comparator.naturalOrder()).toList();
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<TextRenderInfo> chunksOf(final @NotNull PdfDocument document, final int page) {
        final @NotNull List<TextRenderInfo> chunks = new ArrayList<>();
        new PdfCanvasProcessor(new IEventListener() {
            @Override
            public void eventOccurred(final @NotNull IEventData data, final @NotNull EventType type) {
                if (data instanceof final TextRenderInfo info) {
                    info.preserveGraphicsState();
                    chunks.add(info);
                }
            }

            @Override
            public @NotNull Set<EventType> getSupportedEvents() {
                return Set.of(EventType.RENDER_TEXT);
            }
        }).processPageContent(document.getPage(page));
        return chunks;
    }

    private static float startOf(final @NotNull TextRenderInfo chunk) {
        return chunk.getBaseline().getStartPoint().get(Vector.I1);
    }

    private static float endOf(final @NotNull TextRenderInfo chunk) {
        return chunk.getBaseline().getEndPoint().get(Vector.I1);
    }

    private static float heightOf(final @NotNull TextRenderInfo chunk) {
        return chunk.getBaseline().getStartPoint().get(Vector.I2);
    }

    private static @NotNull List<Float> centersOfTheHeaderCells(final @NotNull List<TextRenderInfo> line) {
        final @NotNull List<TextRenderInfo> sorted = line.stream().filter(chunk -> !chunk.getText().isBlank()).sorted(Comparator.comparingDouble(ReportDeliveryIdeTest::startOf)).toList();
        final @NotNull List<Float> centers = new ArrayList<>();
        float from = startOf(sorted.getFirst());
        float to = endOf(sorted.getFirst());
        for (final TextRenderInfo chunk : sorted.subList(1, sorted.size())) {
            if (startOf(chunk) - to > 8) {
                centers.add((from + to) / 2);
                from = startOf(chunk);
            }
            to = endOf(chunk);
        }
        centers.add((from + to) / 2);
        return centers;
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
        wasBound = bound().name();
        bound().choose(TEST_PROJECT);
        testin = folder("testin");
        settings().rootTestinPath = testin.toString();

        WriteAction.runAndWait(() -> {
            final @NotNull NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            testProject = mapper.setTestProjectNode(testin.resolve(TEST_PROJECT));
            Services.getInstance(getProject(), Nodes.class).addTestProject(testProject);
            login = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            Services.getInstance(getProject(), Nodes.class).addTestSet(login);
        });
        aTestCase(opens, "Open the app and log in with a valid user whose password has not expired yet");
        aTestCase(locks, "Lock the account after three wrong passwords in a row, and say so on the login page");

        said = Said.listening(getProject(), getTestRootDisposable()).notifications();
    }

    @Override
    protected void tearDown() {
        try {
            XmlSerializerUtil.copyBean(wasStored, settings());
            bound().choose(wasBound);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull Path folder(final @NotNull String name) {
        try {
            return Files.createDirectories(root.resolve(name));
        } catch (final IOException ex) {
            throw new AssertionError("could not make " + name + ": " + ex.getMessage(), ex);
        }
    }

    private void aTestCase(final @NotNull UUID testCaseUuid, final @NotNull String description) {
        final @NotNull TestCase testCase = TestCase.builder().uuid(testCaseUuid).description(description).order(testCaseUuid.toString()).build();
        testCase.setParent(login);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(login.getPath(), testCase);
    }

    private @NotNull TestRunNode aTestRun(final @NotNull String named, final @NotNull List<RunItem> runItems) {
        final @NotNull TestRunNode testRun = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode created = Services.getInstance(getProject(), NodeMapper.class).setTestRunNode(testProject.getTestRunsFolder().getPath().resolve(named), testProject.getTestRunsFolder());
            Services.getInstance(getProject(), Nodes.class).addTestRunNode(created);
            return created;
        });
        Services.getInstance(getProject(), TestRuns.class).putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(runItems)));
        return testRun;
    }

    private @NotNull TestRunNode theTestRun() {
        return aTestRun("Cycle-1", List.of(
                new RunItem().setUuid(opens).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                new RunItem().setUuid(locks).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The account stayed open").build()))));
    }

    private @NotNull RunItems resultsOf(final @NotNull TestRunNode testRun) {
        return Services.getInstance(getProject(), TestRuns.class).getRunItems(testRun.getPath());
    }

    private @NotNull GenerateReportAction theAction() {
        return new GenerateReportAction(getProject(), new SimpleTree());
    }

    private @NotNull File aReportWritten(final @NotNull FileFormat format, final @NotNull String name) {
        final @NotNull File file = folder("reports").resolve(name).toFile();
        theAction().writeReport(theTestRun(), format, file, new EmptyProgressIndicator());
        return file;
    }

    private @NotNull Notification theReportMessage() {
        Await.until("the report was written and nothing said so", () -> said.stream().anyMatch(notification -> notification.getContent().contains(Bundle.message("report.generated.message", "").strip())));
        return said.stream().filter(notification -> notification.getContent().contains(Bundle.message("report.generated.message", "").strip())).findFirst().orElseThrow();
    }

    private void clicked(final @NotNull Notification message, final @NotNull AnAction link) {
        Notified.press(getProject(), message, link);
    }

    // Rule-REPORT-001
    public void testAReportIsAboutExactlyOneTestRun() {
        final @NotNull TestRunNode chosen = theTestRun();
        aTestCase(UUID.randomUUID(), "Reset a forgotten password");
        final @NotNull TestRunNode other = aTestRun("Cycle-2", List.of(new RunItem().setUuid(locks).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.BLOCKED).build()))));

        final @NotNull String html = new String(FileFormat.HTML.generateReport(getProject(), chosen, resultsOf(chosen)), StandardCharsets.UTF_8);

        assertTrue("the report does not name its test run", html.contains(chosen.getName()));
        assertFalse("the report names a second test run", html.contains(other.getName()));
        assertFalse("the report carries a test case the test run never held", html.contains("Reset a forgotten password"));
    }

    // Rule-REPORT-003
    public void testStoppingTheReportLeavesNoFileBehind() {
        final @NotNull File file = folder("reports").resolve("stopped.pdf").toFile();
        final @NotNull ProgressIndicator stopped = new EmptyProgressIndicator();
        stopped.cancel();

        try {
            theAction().writeReport(theTestRun(), FileFormat.PDF, file, stopped);
            fail("the report went on after it was stopped");
        } catch (final ProcessCanceledException expected) {
            assertFalse("a stopped report left a file behind", file.exists());
        }
        assertTrue("a stopped report said it was written", said.isEmpty());
    }

    // Rule-REPORT-004
    public void testAReportIsWrittenWhereTheTesterChoseAndNeverUnderTheTestinFolder() {
        final @NotNull TestRunNode testRun = theTestRun();
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
        final @NotNull List<Path> before = everythingUnder(testin);
        final @NotNull File file = folder("reports").resolve("Cycle-1.pdf").toFile();

        theAction().writeReport(testRun, FileFormat.PDF, file, new EmptyProgressIndicator());

        assertTrue("the report is not where the tester chose", file.isFile());
        assertEquals("the report put something under the Testin folder", before, everythingUnder(testin));
    }

    // Rule-REPORT-009
    public void testTheDialogClosesBeforeTheWorkStarts() {
        settings().defaultDownloadFolder = folder("reports").toString();
        final @NotNull List<Boolean> openWhenTheWorkStarted = new ArrayList<>();
        final @NotNull Project project = getProject();
        final @NotNull GenerateReportDialog dialog = new GenerateReportDialog(project, "Cycle-1", (_, _) -> openWhenTheWorkStarted.add(ShownDialog.isOpen(project, GenerateReportDialog.class)));

        dialog.show();
        assertTrue("the dialog never opened", ShownDialog.isOpen(project, GenerateReportDialog.class));
        dialog.submit();

        assertEquals("the work started while the dialog was still open", List.of(false), openWhenTheWorkStarted);
    }

    // Rule-REPORT-010
    public void testTheLinkIsOnTheMessageThatSaysTheReportWasWrittenAndTheMessageStaysInTheList() {
        aReportWritten(FileFormat.PDF, "Cycle-1.pdf");
        final @NotNull Notification message = theReportMessage();

        theLink(message, Bundle.message("report.open"));
        assertEquals("the report message is not a lasting notification", NotificationType.INFORMATION, message.getType());
        assertTrue("the report message does not stay in the IDE's notification list", Optional.ofNullable(NotificationGroupManager.getInstance().getNotificationGroup(message.getGroupId())).orElseThrow().isLogByDefault());
    }

    // Rule-REPORT-011
    public void testClickingOpenMakesTheMessageGo() {
        final @NotNull File file = aReportWritten(FileFormat.PDF, "Cycle-1.pdf");
        final @NotNull Notification message = theReportMessage();
        assertTrue("could not take the report away so that clicking Open launches nothing", file.delete());

        clicked(message, theLink(message, Bundle.message("report.open")));

        assertTrue("clicking Open left the message standing", message.isExpired());
    }

    // Rule-REPORT-012
    public void testTheFileIsHandedToTheApplicationThatClaimsIt() {
        final @NotNull List<String> handedOver = new ArrayList<>();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), BrowserLauncher.class, new BrowserLauncher() {
            @Override
            public void open(final @NotNull String url) {
                handedOver.add(url);
            }

            @Override
            public void browse(final @NotNull File file) {
                handedOver.add(file.toURI().toString());
            }

            @Override
            public void browse(final @NotNull Path file) {
                handedOver.add(file.toUri().toString());
            }

            @Override
            public void browse(final @NotNull String url, final @Nullable WebBrowser browser, final @Nullable Project project) {
                handedOver.add(url);
            }
        }, getTestRootDisposable());
        final @NotNull File file = aReportWritten(FileFormat.HTML, "Cycle-1.html");
        final @NotNull Notification message = theReportMessage();

        clicked(message, theLink(message, Bundle.message("report.open")));

        Await.until("the web page was never handed to the application that opens it", () -> !handedOver.isEmpty());
        assertEquals("the report was handed over as something other than its own file", List.of(file.toURI().toString()), handedOver);
    }

    // Rule-REPORT-013, Rule-REPORT-011
    public void testCopyPathCopiesTheWholePathAndMakesTheMessageGo() {
        final @NotNull File file = aReportWritten(FileFormat.PDF, "Cycle-1.pdf");
        final @NotNull Notification message = theReportMessage();

        clicked(message, theLink(message, Bundle.message("notification.copy.path")));

        assertEquals("the copied text is not the whole path", file.getAbsolutePath(), CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
        assertTrue("clicking Copy path left the message standing", message.isExpired());
    }

    // Rule-REPORT-015
    public void testTheReportMessageOffersTheSameCopyLinkAsEveryFileTestinWrote() {
        final @NotNull File file = aReportWritten(FileFormat.PDF, "Cycle-1.pdf");
        final @NotNull AnAction shared = ExportNotice.copyPath(getProject(), file);

        final @NotNull AnAction offered = theLink(theReportMessage(), Bundle.message("notification.copy.path"));

        assertEquals("the report's copy link is not the one every written file carries", shared.getTemplatePresentation().getIcon(), offered.getTemplatePresentation().getIcon());
        assertEquals("the report's copy link does not read the same", shared.getTemplatePresentation().getText(), offered.getTemplatePresentation().getText());
    }

    // Rule-REPORT-024
    public void testTheNumberAndTheRunItemStatusTakeOnlyTheirOwnWidthAndTheDescriptionTheRest() {
        final @NotNull TestRunNode testRun = theTestRun();

        final @NotNull String html = new String(FileFormat.HTML.generateReport(getProject(), testRun, resultsOf(testRun)), StandardCharsets.UTF_8);
        assertTrue("the web page does not give the number only its own width", html.contains(".detail-table td.seq, .detail-table th.seq { width: 1%; white-space: nowrap; }") && html.contains("<th class='seq'>#</th>"));
        assertTrue("the web page does not give the run item status only its own width", html.contains(".detail-table td.run-item-status, .detail-table th.run-item-status { width: 1%; white-space: nowrap; }") && html.contains("<th class='run-item-status'>"));
        assertTrue("the web page fixes a width on the description", html.contains("<th>" + Bundle.message("caption.test.case") + "</th>"));

        final @NotNull List<Float> widths = pdfColumnWidths(FileFormat.PDF.generateReport(getProject(), testRun, resultsOf(testRun)));
        assertEquals("the PDF's failed test case table does not have its four columns: " + widths, 4, widths.size());
        final float number = widths.getFirst();
        final float description = widths.get(1);
        assertTrue("in the PDF the number is " + number + " wide and the description " + description + ": " + widths, number * 4 < description);
        assertTrue("in the PDF the bug priority is " + widths.get(2) + " wide and the description " + description, widths.get(2) < description);
        assertTrue("in the PDF the bug severity is " + widths.get(3) + " wide and the description " + description, widths.get(3) < description);
    }

    private @NotNull List<Float> pdfColumnWidths(final byte @NotNull [] pdf) {
        final @NotNull String severity = TestRunEditorFields.BUG_SEVERITY.getName();
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                final @NotNull List<TextRenderInfo> chunks = chunksOf(document, page);
                final @NotNull Optional<TextRenderInfo> header = chunks.stream().filter(chunk -> !chunk.getText().isBlank() && severity.startsWith(chunk.getText().strip())).reduce((_, later) -> later);
                final @NotNull Optional<TextRenderInfo> description = chunks.stream().filter(chunk -> chunk.getText().strip().startsWith("Lock the")).findFirst();
                if (header.isEmpty() || description.isEmpty()) continue;

                final float y = heightOf(header.orElseThrow());
                final @NotNull List<Float> centers = centersOfTheHeaderCells(chunks.stream().filter(chunk -> Math.abs(heightOf(chunk) - y) < 2).toList());
                float edge = startOf(description.orElseThrow()) - 6;
                final @NotNull List<Float> widths = new ArrayList<>();
                widths.add(2 * (edge - centers.getFirst()));
                for (final float center : centers.subList(1, centers.size())) {
                    final float width = 2 * (center - edge);
                    widths.add(width);
                    edge += width;
                }
                return widths;
            }
            return List.of();
        } catch (final IOException ex) {
            throw new AssertionError("could not read the PDF back: " + ex.getMessage(), ex);
        }
    }
}

