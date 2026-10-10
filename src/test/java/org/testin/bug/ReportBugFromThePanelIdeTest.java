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
package org.testin.bug;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.config.TestinYml;
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Done;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.BrowserOpened;
import org.testin.view.Drawn;
import org.testin.view.KeyPress;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class ReportBugFromThePanelIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String REPOSITORY = "https://example.invalid/acme/shop";
    private static final @NotNull String ISSUE = "https://example.invalid/acme/shop/issues/7";
    private static final @NotNull KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    private final @NotNull List<List<String>> asked = new CopyOnWriteArrayList<>();
    private final @NotNull List<String> bodies = new CopyOnWriteArrayList<>();
    private final @NotNull List<ProcessOutput> answers = new CopyOnWriteArrayList<>();

    private TestCaseDto tc;
    private TestRunNode tr;
    private RunItem failed;
    private RunItemPath runItemPath;
    private Optional<String> ymlBefore = Optional.empty();
    private Function<ProgressIndicator, GitHubCli> ghBefore;

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static <T> @NotNull T theOne(final @NotNull JComponent dialog, final @NotNull Class<T> kind) {
        final @NotNull List<T> found = Drawn.components(dialog).stream().filter(kind::isInstance).map(kind::cast).toList();
        assertEquals("the dialog does not hold exactly one " + kind.getSimpleName(), 1, found.size());
        return found.getFirst();
    }

    private static @NotNull JTextField title(final @NotNull JComponent dialog) {
        return theOne(dialog, JTextField.class);
    }

    private static @NotNull JTextArea body(final @NotNull JComponent dialog) {
        return theOne(dialog, JTextArea.class);
    }

    private static @NotNull JButton send(final @NotNull JComponent dialog) {
        return Drawn.components(dialog).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> Bundle.message("bug.dialog.send").equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no Send"));
    }

    private static @NotNull String besideSend(final @NotNull JComponent dialog) {
        final @NotNull JButton send = send(dialog);
        return Arrays.stream(send.getParent().getComponents()).filter(part -> !part.equals(send)).map(Drawn::text).filter(text -> !text.isEmpty()).findFirst().orElse("");
    }

    private static void enterBindingsFrom(final @NotNull Component holder) {
        if (holder instanceof final JComponent component) {
            for (final InputMap keys : List.of(component.getInputMap(JComponent.WHEN_FOCUSED), component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT))) {
                Optional.ofNullable(keys.get(ENTER)).map(name -> component.getActionMap().get(name))
                        .ifPresent((final Action action) -> action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "Enter")));
            }
        }
        Optional.ofNullable(holder.getParent()).ifPresent(ReportBugFromThePanelIdeTest::enterBindingsFrom);
    }

    @Override
    protected void setUp() {
        super.setUp();
        ymlBefore = TestinYml.savePath(getProject()).filter(Files::isRegularFile).map(ReportBugFromThePanelIdeTest::read);
        ghBefore = BugFiling.ghOnThisMachine;
        BugFiling.ghOnThisMachine = _ -> new GitHubCli(this::answer);

        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        tc = EditorFixtures.testCase(getProject(), EditorFixtures.testSet(getProject(), tp, "Login"), "Log in with a valid user", "a");
        failed = RunItem.builder().id(tc.getId()).status(RunItemStatus.FAILED).actualResult("The session was dropped").bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).build();
        tr = EditorFixtures.testRun(getProject(), tp, List.of(failed));
        runItemPath = new RunItemPath(tr.getPath(), tc.getId());
    }

    @Override
    protected void tearDown() {
        try {
            ShownDialog.close(getProject(), ReportBugDialog.class);
            BugFiling.ghOnThisMachine = ghBefore;
            restoreTheYml();
        } finally {
            super.tearDown();
        }
    }

    private void restoreTheYml() {
        TestinYml.savePath(getProject()).ifPresent(file -> {
            try {
                if (ymlBefore.isPresent()) Files.writeString(file, ymlBefore.orElseThrow(), StandardCharsets.UTF_8);
                else Files.deleteIfExists(file);
            } catch (final IOException ex) {
                throw new AssertionError("Could not put testin.yml back: " + ex.getMessage(), ex);
            }
        });
        TestinYml.reload(getProject());
    }

    private @NotNull Optional<ProcessOutput> answer(final @NotNull List<String> arguments, final @NotNull Path workDirectory) {
        asked.add(arguments);
        final @NotNull Path body = workDirectory.resolve("bug.md");
        if (Files.isRegularFile(body)) bodies.add(read(body));
        return answers.isEmpty() ? Optional.empty() : Optional.of(answers.removeFirst());
    }

    private @NotNull List<List<String>> issuesCreated() {
        return asked.stream().filter(arguments -> arguments.size() > 1 && arguments.getFirst().equals("issue") && arguments.get(1).equals("create")).toList();
    }

    private @NotNull PreparedBug aPreparedBug(final @NotNull Optional<String> whyNotReady) {
        return new PreparedBug(BugFacts.of(failed, tc, tr.getMarker(), tr.getName(), List.of()), "The template body", BugRepository.of(REPOSITORY), whyNotReady);
    }

    private @NotNull JComponent opened(final @NotNull PreparedBug bug) {
        final @NotNull BugReports reports = Services.getInstance(getProject(), BugReports.class);
        reports.begin(runItemPath);
        reports.moveTo(runItemPath, Stage.OPEN);
        new ReportBugDialog(getProject(), runItemPath, bug, () -> {
        }).open();
        return ShownDialog.content(getProject(), ReportBugDialog.class);
    }

    private void sentAndSettled() {
        Await.until("the send never finished", () -> Services.getInstance(getProject(), BugReports.class).whyReportBugIsOff(runItemPath, failed).filter(Bundle.message("bug.sending")::equals).isEmpty() && !issuesCreated().isEmpty());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private void pressEnterIn(final @NotNull JComponent field) {
        field.dispatchEvent(new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
        enterBindingsFrom(field);
        for (final AnAction action : KeyPress.answering(field, ENTER))
            ActionUtil.performAction(action, KeyPress.eventIn(getProject(), action, field));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    // Rule-VIEW-PANEL-067, Rule-VIEW-PANEL-078
    public void testReportBugReadsTestinYmlAgainAndPreparesTheBugUnderTheProgressBar() {
        TestinYml.savePath(getProject()).map(Path::getParent).ifPresent(folder -> {
            try {
                Files.createDirectories(folder);
            } catch (final IOException ex) {
                throw new AssertionError("Could not make the project's folder: " + ex.getMessage(), ex);
            }
        });
        assertTrue("testin.yml could not be written", TestinYml.save(getProject(), TestinYml.lines("NAFATH")));
        TestinYml.savePath(getProject()).ifPresent(file -> {
            try {
                Files.writeString(file, "testinProject: NAFATH\nbugRepoUrl: " + REPOSITORY + "\n", StandardCharsets.UTF_8);
            } catch (final IOException ex) {
                throw new AssertionError("Could not write testin.yml: " + ex.getMessage(), ex);
            }
        });
        assertEquals("the test did not leave Testin's copy of testin.yml behind the file", "", TestinYml.bugRepoUrl(getProject()));
        final @NotNull List<String> underProgress = new CopyOnWriteArrayList<>();
        BackgroundWork.watch(getTestRootDisposable(), task -> {
            underProgress.add(task.getTitle());
            return false;
        });

        ReportBug.start(getProject(), tr, tc.getId(), tc, () -> {
        });

        Await.until("Report Bug never opened the bug", () -> ShownDialog.isOpen(getProject(), ReportBugDialog.class));
        assertTrue("the bug was not prepared under the IDE's progress bar: " + underProgress, underProgress.contains(Bundle.message("bug.preparing")));
        final @NotNull List<String> words = Drawn.words(ShownDialog.content(getProject(), ReportBugDialog.class));
        assertTrue("the repository is not the one bugRepoUrl in testin.yml names now: " + words, Drawn.holds(words, "acme/shop"));
    }

    // Rule-VIEW-PANEL-067
    public void testStoppingTheProgressBarOpensNothing() {
        BackgroundWork.watch(getTestRootDisposable(), task -> Bundle.message("bug.preparing").equals(task.getTitle()));

        ReportBug.start(getProject(), tr, tc.getId(), tc, () -> {
        });

        Await.until("the stopped preparation never finished", () -> Services.getInstance(getProject(), BugReports.class).whyReportBugIsOff(runItemPath, failed).isEmpty());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertFalse("stopping the progress bar still opened the bug", ShownDialog.isOpen(getProject(), ReportBugDialog.class));
    }

    // Rule-VIEW-PANEL-069
    public void testTheTitleAndTheBodyCanBeEditedAndSendFilesExactlyWhatTheDialogHolds() {
        final @NotNull JComponent dialog = opened(aPreparedBug(Optional.empty()));
        assertTrue("the title cannot be edited", title(dialog).isEditable());
        assertTrue("the body cannot be edited", body(dialog).isEditable());

        title(dialog).setText("Log in drops the session");
        body(dialog).setText("What the tester wrote instead");
        answers.add(new ProcessOutput(ISSUE + "\n", "", 0, false, false));
        send(dialog).doClick();
        sentAndSettled();

        final @NotNull List<String> arguments = issuesCreated().getFirst();
        assertTrue("Send filed another title: " + arguments, arguments.contains("--title=Log in drops the session"));
        assertTrue("Send filed into another repository: " + arguments, arguments.contains("example.invalid/acme/shop"));
        assertEquals("Send filed another body", List.of("What the tester wrote instead"), bodies);
    }

    // Rule-VIEW-PANEL-070
    public void testOnlyAClickOnSendSendsAndItSendsOnce() {
        final @NotNull JComponent dialog = opened(aPreparedBug(Optional.empty()));

        pressEnterIn(title(dialog));
        pressEnterIn(body(dialog));
        assertEquals("Enter sent the bug", List.of(), issuesCreated());
        assertTrue("Enter closed the dialog", ShownDialog.isOpen(getProject(), ReportBugDialog.class));

        answers.add(new ProcessOutput(ISSUE + "\n", "", 0, false, false));
        answers.add(new ProcessOutput(ISSUE + "\n", "", 0, false, false));
        final @NotNull JButton send = send(dialog);
        send.doClick();
        send.doClick();
        sentAndSettled();
        assertEquals("one Send filed the bug more than once", 1, issuesCreated().size());

        opened(aPreparedBug(Optional.empty()));
        ShownDialog.press(getProject(), ReportBugDialog.class, Shortcuts.Escape);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertFalse("Escape did not close the dialog", ShownDialog.isOpen(getProject(), ReportBugDialog.class));
        assertFalse("Escape asked before closing", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
        assertEquals("Escape sent the bug", 1, issuesCreated().size());
    }

    // Rule-VIEW-PANEL-071
    public void testSendIsGrayWhenTheBugCannotBeSentAndHoveringSaysWhy() {
        final @NotNull String noGh = Bundle.message("bug.reason.no.gh");
        final @NotNull JComponent notReady = opened(aPreparedBug(Optional.of(noGh)));
        assertFalse("Send is not gray while gh cannot send", send(notReady).isEnabled());
        assertEquals("hovering over the gray Send does not say why", noGh, Drawn.hovering(send(notReady)));
        ShownDialog.close(getProject(), ReportBugDialog.class);

        final @NotNull JComponent ready = opened(aPreparedBug(Optional.empty()));
        assertTrue("Send is gray on a bug that can be sent", send(ready).isEnabled());
        assertEquals("hovering over a Send that can be pressed gives a reason it cannot", "", Drawn.hovering(send(ready)));
        title(ready).setText(" ");
        assertFalse("Send is not gray with an empty title", send(ready).isEnabled());
        assertEquals("hovering over the gray Send does not say why", Bundle.message("bug.limit.title.empty"), Drawn.hovering(send(ready)));
        assertEquals("the reason is printed beside Send instead of only on hover (Rule-INTERNAL-080)", "", besideSend(ready));
    }

    // Rule-VIEW-PANEL-077
    public void testEditsThatWereNotSentComeBackUntilTheyAreSentOrThrownAway() {
        final @NotNull JComponent first = opened(aPreparedBug(Optional.empty()));
        title(first).setText("Log in drops the session");
        body(first).setText("What the tester wrote instead");
        answers.add(new ProcessOutput("", "HTTP 422: Validation Failed", 1, false, false));
        send(first).doClick();
        sentAndSettled();

        final @NotNull JComponent afterAFailedSend = opened(aPreparedBug(Optional.empty()));
        assertEquals("the title the tester wrote did not come back after a failed send", "Log in drops the session", title(afterAFailedSend).getText());
        assertEquals("the body the tester wrote did not come back after a failed send", "What the tester wrote instead", body(afterAFailedSend).getText());

        ShownDialog.press(getProject(), ReportBugDialog.class, Shortcuts.Escape);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JComponent afterThrowingAway = opened(aPreparedBug(Optional.empty()));
        assertEquals("edits thrown away came back", "The template body", body(afterThrowingAway).getText());

        body(afterThrowingAway).setText("Written once more");
        answers.add(new ProcessOutput(ISSUE + "\n", "", 0, false, false));
        send(afterThrowingAway).doClick();
        Await.until("the second send never happened", () -> issuesCreated().size() == 2);
        sentAndSettled();
        final @NotNull JComponent afterSending = opened(aPreparedBug(Optional.empty()));
        assertEquals("edits that were sent came back", "The template body", body(afterSending).getText());
    }

    // Rule-VIEW-PANEL-078
    public void testTestinKeepsNoPasswordOrTokenAndHandsGhNone() {
        final @NotNull List<String> secrets = new ArrayList<>();
        for (final Field field : AppSettingsState.class.getDeclaredFields()) {
            final @NotNull String name = field.getName().toLowerCase(Locale.ROOT);
            if (name.contains("token") || name.contains("password") || name.contains("secret"))
                secrets.add(field.getName());
        }
        assertEquals("Testin's settings keep a password or a token", List.of(), secrets);

        final @NotNull JComponent dialog = opened(aPreparedBug(Optional.empty()));
        answers.add(new ProcessOutput(ISSUE + "\n", "", 0, false, false));
        send(dialog).doClick();
        sentAndSettled();
        assertTrue("gh was handed a token: " + asked, asked.stream().flatMap(List::stream).noneMatch(argument -> argument.toLowerCase(Locale.ROOT).contains("token")));
    }

    // Rule-VIEW-PANEL-089
    public void testTheReportedMessageKeepsItsOpenAfterItIsPressed() {
        final @NotNull BrowserOpened browser = BrowserOpened.recording(getTestRootDisposable());
        final @NotNull List<Notification> shown = Said.listening(getProject(), getTestRootDisposable()).notifications();

        BugFiling.record(getProject(), runItemPath, new IssueCreation(Optional.of(ISSUE), 0, ""));

        final @NotNull Notification reported = shown.stream().filter(notification -> notification.getTitle().equals(Done.REPORTED.getOutcome())).findFirst().orElseThrow(() -> new AssertionError("no message said the bug was reported"));
        final @NotNull AnAction open = reported.getActions().stream().filter(action -> Bundle.message("bug.open.issue").equals(action.getTemplateText())).findFirst()
                .orElseThrow(() -> new AssertionError("the message has no Open"));

        Notified.press(getProject(), reported, open);
        assertFalse("pressing Open took Open away", reported.isExpired());
        assertTrue("Open is gone from the message after it was pressed", reported.getActions().contains(open));
        Notified.press(getProject(), reported, open);

        assertEquals("Open did not open the issue each time it was pressed", List.of(ISSUE, ISSUE), browser.addresses());
    }
}
