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

package org.testin.codegen;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.IdeView;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.ActiveComponent;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.OnScreen;
import org.testin.Said;
import org.testin.SpellChecking;
import org.testin.TestinLog;
import org.testin.config.TestinYml;
import org.testin.logger.LogLevel;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.Fonts;
import org.testin.util.Json;
import org.testin.view.Drawn;
import org.testin.view.PopupsBuilt;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ApiModelActionIdeTest extends AbstractCodegenIdeTest {
    private static final @NotNull String ACTION = "Testin.ApiModel";

    private static final @NotNull String REMEMBERED_LAYOUT = "testin.apiModel.sideBySide";

    private static final @NotNull String REQUEST_BODY = "{\"user\":\"ada\",\"password\":\"secret\"}";

    private static final @NotNull String RESPONSE_BODY = "{\"token\":\"t-1\",\"expiresIn\":3600}";

    @Override
    protected void tearDown() {
        try {
            PropertiesComponent.getInstance().unsetValue(REMEMBERED_LAYOUT);
        } finally {
            super.tearDown();
        }
    }

    private static @NotNull List<EditorTextField> boxes(final @NotNull JComponent dialog) {
        return Drawn.components(dialog).stream().filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).toList();
    }

    private static void filledIn(final @NotNull JComponent dialog, final @NotNull String name, final @NotNull String request) {
        Drawn.first(dialog, JTextComponent.class, JTextComponent::isEditable).setText(name);
        boxes(dialog).getFirst().setText(request);
        boxes(dialog).getLast().setText(RESPONSE_BODY);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static void created(final @NotNull JComponent dialog) {
        Drawn.first(dialog, JButton.class, button -> Bundle.message("dialog.api.model.create").equals(button.getText())).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static @NotNull Optional<String> textOf(final @NotNull PsiDirectory directory, final @NotNull String fileName) {
        return ReadAction.computeBlocking(() -> Optional.ofNullable(directory.findFile(fileName)).map(PsiFile::getText));
    }

    private static @NotNull JButton layoutSwitch(final @NotNull PopupsBuilt.Built dialog) {
        final @NotNull JComponent titleBar = ((ActiveComponent) dialog.asked("setCommandButton").getFirst()).getComponent();
        final @NotNull List<String> names = List.of(Bundle.message("dialog.pair.side.by.side"), Bundle.message("dialog.pair.stacked"));
        return Drawn.first(titleBar, JButton.class, button -> names.contains(button.getAccessibleContext().getAccessibleName()));
    }

    private static @NotNull Component headerOf(final @NotNull Component inside) {
        return inside.isCursorSet() ? inside : headerOf(Objects.requireNonNull(inside.getParent(), "the section has no line to click"));
    }

    private static void clickTheLineOf(final @NotNull JComponent dialog, final @NotNull String caption) {
        final @NotNull Component header = headerOf(Drawn.first(dialog, JBLabel.class, label -> label.getText().startsWith(caption)));
        for (final MouseListener listener : header.getMouseListeners())
            listener.mouseClicked(new MouseEvent(header, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false));
    }

    private static boolean showsIn(final @NotNull JComponent dialog, final @NotNull Component component) {
        return component.equals(dialog) || (component.isVisible() && showsIn(dialog, Objects.requireNonNull(component.getParent(), "the box is not in the dialog")));
    }

    private @NotNull PopupsBuilt.Built opened(final @NotNull PsiDirectory directory) {
        final @NotNull PopupsBuilt popups = PopupsBuilt.recording(getTestRootDisposable());
        ApiModelMaker.available().orElseThrow(() -> new AssertionError("no API model maker is loaded")).open(getProject(), directory);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return popups.last();
    }

    private @NotNull Editor drawn(final @NotNull EditorTextField box) {
        Await.until("the box never drew its editor", () -> box.getEditor() != null);
        return Objects.requireNonNull(box.getEditor());
    }


    private @NotNull AnAction action() {
        return Optional.ofNullable(ActionManager.getInstance().getAction(ACTION))
                .orElseThrow(() -> new AssertionError(ACTION + " is not registered in plugin.xml"));
    }

    private @NotNull VirtualFile sourceRoot() {
        return Arrays.stream(ModuleRootManager.getInstance(getModule()).getSourceRoots(true)).findFirst()
                .orElseThrow(() -> new AssertionError("the project has no source folder"));
    }

    private @NotNull PsiDirectory directoryOf(final @NotNull VirtualFile folder) {
        return Optional.ofNullable(PsiManager.getInstance(getProject()).findDirectory(folder)).orElseThrow(() -> new AssertionError("no directory for " + folder));
    }

    private @NotNull PsiDirectory aPackage() {
        return WriteAction.computeAndWait(() -> directoryOf(sourceRoot()).createSubdirectory("api"));
    }

    private @NotNull PsiDirectory aFolderOutsideTheSources() {
        return directoryOf(Objects.requireNonNull(sourceRoot().getParent(), "the source folder has no parent"));
    }

    private @NotNull Presentation shownOn(final @NotNull PsiDirectory directory) {
        final @NotNull AnAction action = action();
        final @NotNull IdeView view = new IdeView() {
            @Override
            public PsiDirectory @NotNull [] getDirectories() {
                return new PsiDirectory[]{directory};
            }

            @Override
            public @NotNull PsiDirectory getOrChooseDirectory() {
                return directory;
            }
        };
        final @NotNull AnActionEvent e = AnActionEvent.createEvent(action,
                SimpleDataContext.builder().add(CommonDataKeys.PROJECT, getProject()).add(LangDataKeys.IDE_VIEW, view).build(),
                action.getTemplatePresentation().clone(), ActionPlaces.PROJECT_VIEW_POPUP, ActionUiKind.POPUP, null);
        ActionUtil.updateAction(action, e);
        return e.getPresentation();
    }

    // UC-CODEGEN-022, Rule-CODEGEN-098
    public void testTheEntryCarriesItsName() {
        assertEquals(Bundle.message("action.Testin.ApiModel.text"), action().getTemplatePresentation().getText());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-098
    public void testOnAJavaPackageItCanBeChosen() {
        assertTrue("the entry is gray on a Java package", shownOn(aPackage()).isEnabled());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-098
    public void testOnAFolderThatIsNotAPackageItIsGrayAndSaysWhy() {
        final @NotNull Presentation shown = shownOn(aFolderOutsideTheSources());

        assertFalse("the entry works on a folder that is not a Java package", shown.isEnabled());
        assertEquals(Bundle.message("api.model.not.a.package"), shown.getDescription());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-082, Rule-CODEGEN-098
    public void testWhileTestinYmlNamesAnotherTestProjectItIsGrayAndSaysItNeedsIt() {
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);
        try {
            bound.choose("Shop");

            final @NotNull Presentation shown = shownOn(aPackage());

            assertFalse("the entry works while testin.yml names another test project", shown.isEnabled());
            assertTrue("the entry does not say it needs testin.yml: " + shown.getText(), Objects.toString(shown.getText(), "").contains(TestinYml.fileName()));
        } finally {
            bound.choose(TestinYml.testProjectName(getProject()));
        }
    }

    // Rule-CODEGEN-105
    public void testCreateOpensBothFilesSaysCreatedAndOneUndoTakesThemBothBack() {
        final @NotNull PsiDirectory api = aPackage();
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        final @NotNull JComponent dialog = opened(api).getContent();
        filledIn(dialog, "Login", REQUEST_BODY);

        created(dialog);

        final @NotNull List<String> open = Arrays.stream(FileEditorManager.getInstance(getProject()).getOpenFiles()).map(VirtualFile::getName).toList();
        assertTrue("the files did not open in the editor: " + open, open.containsAll(List.of("LoginRequest.java", "LoginResponse.java")));
        assertEquals(List.of(Done.CREATED.getOutcome()), balloons);

        UndoManager.getInstance(getProject()).undo(null);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("one Undo did not take both files back", List.of(Optional.empty(), Optional.empty()), List.of(textOf(api, "LoginRequest.java"), textOf(api, "LoginResponse.java")));
    }

    // Rule-CODEGEN-105
    public void testAFileAlreadyInThePackageIsNeverReplaced() {
        final @NotNull PsiDirectory api = aPackage();
        final @NotNull String kept = "package api;\n\npublic class LoginRequest {\n}\n";
        WriteCommandAction.writeCommandAction(getProject()).run(() -> api.add(PsiFileFactory.getInstance(getProject()).createFileFromText("LoginRequest.java", JavaFileType.INSTANCE, kept)));
        final @NotNull JComponent dialog = opened(api).getContent();
        filledIn(dialog, "Login", REQUEST_BODY);

        created(dialog);

        assertEquals("a file already in the package was replaced", Optional.of(kept), textOf(api, "LoginRequest.java"));
        assertEquals("the other file was written although Create was refused", Optional.empty(), textOf(api, "LoginResponse.java"));
    }

    // Rule-CODEGEN-106
    public void testTheButtonBesideMaximizeSwitchesTheBoxesAndTheNextDialogOpensTheWayItWasLeft() {
        final @NotNull PsiDirectory api = aPackage();
        final @NotNull PopupsBuilt.Built first = opened(api);
        assertEquals("the dialog cannot be resized", true, first.asked("setResizable").getLast());
        final @NotNull JButton layout = layoutSwitch(first);
        assertEquals("the boxes did not start stacked", Bundle.message("dialog.pair.side.by.side"), layout.getAccessibleContext().getAccessibleName());

        layout.doClick();

        assertEquals("the button did not put the boxes side by side", Bundle.message("dialog.pair.stacked"), layout.getAccessibleContext().getAccessibleName());
        first.close();
        assertEquals("the next dialog did not open side by side", Bundle.message("dialog.pair.stacked"), layoutSwitch(opened(api)).getAccessibleContext().getAccessibleName());
    }

    // Rule-CODEGEN-106
    public void testBothSectionsOpenEachTimeAClosedOneIsStillWrittenAndEachNamesItsFile() {
        final @NotNull PsiDirectory api = aPackage();
        final @NotNull JComponent dialog = opened(api).getContent();
        filledIn(dialog, "login", REQUEST_BODY);
        assertTrue("a section's line does not name its file: " + Drawn.words(dialog), Drawn.holds(Drawn.words(dialog), "LoginRequest.java") && Drawn.holds(Drawn.words(dialog), "LoginResponse.java"));

        clickTheLineOf(dialog, Bundle.message("dialog.api.model.response.caption"));
        assertFalse("the Response section did not close", showsIn(dialog, boxes(dialog).getLast()));
        created(dialog);

        assertTrue("the closed Response was not written", textOf(api, "LoginResponse.java").isPresent());
        final @NotNull JComponent again = opened(api).getContent();
        assertTrue("a section opened closed", boxes(again).stream().allMatch(box -> showsIn(again, box)));
    }

    // Rule-CODEGEN-107
    public void testAnEmptyBoxIsThreeLinesAndAPasteGrowsItToItsShareOfTheFrameThenScrollsUncheckedForSpelling() {
        final @NotNull PopupsBuilt.Built built = opened(aPackage());
        final @NotNull JComponent dialog = built.getContent();
        final @NotNull EditorTextField request = boxes(dialog).getFirst();
        final int line = request.getFontMetrics(Fonts.field()).getHeight();
        assertEquals("an empty box is not three lines high", line * 3 + request.getInsets().top + request.getInsets().bottom, request.getPreferredSize().height);
        OnScreen.shown(dialog, getTestRootDisposable());
        final @NotNull Editor editor = drawn(request);

        request.setText(IntStream.range(0, 400).mapToObj(i -> "{\"line\":" + i + "}").collect(Collectors.joining("\n")));
        final int frame = ScreenUtil.getMainScreenBounds().height;
        assertTrue("there is no frame to take a share of", frame > 0);
        assertEquals("stacked, a long paste did not stop at 30% of the frame", frame * 0.30, request.getPreferredSize().height, 1.0);
        layoutSwitch(built).doClick();
        assertEquals("side by side, a long paste did not stop at 60% of the frame", frame * 0.60, request.getPreferredSize().height, 1.0);

        assertTrue("the scrollbar takes a width of its own", ((EditorEx) editor).getScrollPane() instanceof final JBScrollPane pane && pane.isOverlappingScrollBar());
        assertFalse("spelling is checked in the box", SpellChecking.isOn(getProject(), request));
    }

    // Rule-CODEGEN-108
    public void testDebugRecordsEachBodyAsPastedThenEachFileAsWrittenInfoOnlyTheirNamesAndAboveInfoNothing() {
        final @NotNull PsiDirectory api = aPackage();
        final @NotNull TestinLog atDebug = TestinLog.fromNow(getTestRootDisposable());
        TestinLogger.setLogLevel(LogLevel.DEBUG);
        final @NotNull JComponent first = opened(api).getContent();
        filledIn(first, "Login", REQUEST_BODY);
        created(first);

        final @NotNull String debug = atDebug.written();
        final @NotNull String requestFile = textOf(api, "LoginRequest.java").orElseThrow();
        assertTrue("Debug did not record the request as pasted: " + debug, debug.contains(REQUEST_BODY));
        assertTrue("Debug did not record the response as pasted: " + debug, debug.contains(RESPONSE_BODY));
        assertTrue("Debug did not record the file as written, after the bodies: " + debug, debug.indexOf(requestFile) > debug.indexOf(RESPONSE_BODY));

        final @NotNull TestinLog atInfo = TestinLog.fromNow(getTestRootDisposable());
        final @NotNull JComponent second = opened(api).getContent();
        filledIn(second, "Logout", REQUEST_BODY);
        created(second);
        final @NotNull String info = atInfo.written();
        assertTrue("Info did not name the files written: " + info, info.contains("LogoutRequest.java") && info.contains("LogoutResponse.java"));
        assertFalse("Info recorded a body: " + info, info.contains(REQUEST_BODY) || info.contains(RESPONSE_BODY));

        final @NotNull TestinLog aboveInfo = TestinLog.fromNow(getTestRootDisposable());
        TestinLogger.setLogLevel(LogLevel.WARN);
        final @NotNull JComponent third = opened(api).getContent();
        filledIn(third, "Signup", REQUEST_BODY);
        created(third);
        TestinLogger.setLogLevel(LogLevel.INFO);
        assertFalse("above Info something was recorded", aboveInfo.written().contains("Signup"));
    }

    // Rule-CODEGEN-108
    public void testDebugRecordsARefusedBodyWithItsReasonAndReadsNoBodyAfterIt() {
        final @NotNull TestinLog atDebug = TestinLog.fromNow(getTestRootDisposable());
        TestinLogger.setLogLevel(LogLevel.DEBUG);
        final @NotNull JComponent dialog = opened(aPackage()).getContent();
        filledIn(dialog, "Login", "not a body at all");

        created(dialog);

        final @NotNull String debug = atDebug.written();
        assertTrue("Debug did not record the refused body: " + debug, debug.contains("not a body at all"));
        assertTrue("Debug did not record why it was refused: " + debug, debug.contains(Refused.NOT_JSON.about(Bundle.message("dialog.api.model.request"))));
        assertFalse("a body after the refused one was read: " + debug, debug.contains(RESPONSE_BODY));
    }

    // Rule-CODEGEN-109
    public void testTheBoxesAreJsonEditorsAndFormatJsonLaysABodyOutInTheTestersJsonStyle() {
        final @NotNull JComponent dialog = opened(aPackage()).getContent();
        assertEquals("the boxes are not JSON editors", List.of(Json.fileType(), Json.fileType()), boxes(dialog).stream().map(EditorTextField::getFileType).toList());
        final @NotNull EditorTextField request = boxes(dialog).getFirst();
        OnScreen.shown(dialog, getTestRootDisposable());
        request.setText(REQUEST_BODY);
        assertEquals("the body is not drawn in JSON's colors", Json.language(), drawn(request).getHighlighter().createIterator(1).getTokenType().getLanguage());
        final @NotNull JButton format = Drawn.first(dialog, JButton.class, button -> Bundle.message("dialog.box.format", Json.fileType().getDisplayName()).equals(button.getAccessibleContext().getAccessibleName()));

        CodeStyle.runWithLocalSettings(getProject(), CodeStyle.getSettings(getProject()), settings -> {
            settings.getLanguageIndentOptions(Json.language()).INDENT_SIZE = 7;
            format.doClick();
        });

        assertTrue("the body was not laid out in the tester's JSON style: " + request.getText(), request.getText().contains("\n       \"user\""));
        assertEquals("formatting changed more than whitespace", REQUEST_BODY, request.getText().replaceAll("\\s", ""));
    }
}
