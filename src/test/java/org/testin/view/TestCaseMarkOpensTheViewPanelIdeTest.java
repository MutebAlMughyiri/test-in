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

package org.testin.view;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.LoginTestSource;
import org.testin.indexer.TestProjectIndexer;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Bundle;

import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestCaseMarkOpensTheViewPanelIdeTest extends AbstractCodegenIdeTest {

    private ViewOnScreen view;

    private static void clicked(final @NotNull GutterMark mark) {
        clicked(((LineMarkerInfo.LineMarkerGutterIconRenderer<?>) mark).getLineMarkerInfo());
    }

    private static <T extends PsiElement> void clicked(final @NotNull LineMarkerInfo<T> info) {
        final @NotNull T element = Optional.ofNullable(info.getElement()).orElseThrow(() -> new AssertionError("the mark stands beside nothing"));
        Optional.ofNullable(info.getNavigationHandler()).orElseThrow(() -> new AssertionError("the mark does nothing when clicked"))
                .navigate(new MouseEvent(new JPanel(), MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false), element);
    }

    @Override
    protected void setUp() {
        super.setUp();
        view = ViewOnScreen.closed(getProject(), getTestRootDisposable());
    }

    private @NotNull GutterMark theMarkBeside(final @NotNull TestCase testCase) {
        final @NotNull String testCaseUuid = testCase.getUuid().toString();
        final @NotNull String text = LoginTestSource.withTestNg(testCaseUuid);
        final @NotNull PsiFile file = myFixture.addFileToProject("nafath/LoginTest.java", text);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.getEditor().getCaretModel().moveToOffset(text.indexOf(testCaseUuid));

        return myFixture.findGuttersAtCaret().stream()
                .filter(mark -> Bundle.message("gutter.view.details").equals(mark.getTooltipText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the method has no mark beside its identity"));
    }

    private boolean showing(final @NotNull TestCase testCase) {
        return view.getPanel().getCurrentTestCase().filter(shown -> shown.getUuid().equals(testCase.getUuid())).isPresent();
    }

    // Rule-CODEGEN-070
    public void testTheMarkOpensTheViewPanelOnTheTestCaseAndMovesNothingElse() {
        final @NotNull TestCase testCase = indexedTestCase(indexedTestSet("Login", theTestCasesDirectory()), "Log in", "b");
        final @NotNull GutterMark mark = theMarkBeside(testCase);
        final @NotNull FileEditorManager editors = FileEditorManager.getInstance(getProject());
        final @NotNull List<VirtualFile> openBefore = Arrays.asList(editors.getOpenFiles());
        final @NotNull List<VirtualFile> selectedBefore = Arrays.asList(editors.getSelectedFiles());
        final int caretBefore = myFixture.getEditor().getCaretModel().getOffset();

        clicked(mark);
        Await.until("the mark did not open the view panel on its test case", () -> showing(testCase));

        assertTrue("the mark did not open the view panel", view.isOpen());
        assertEquals("the mark opened or closed an editor tab", openBefore, Arrays.asList(editors.getOpenFiles()));
        assertEquals("the mark moved the editor tabs", selectedBefore, Arrays.asList(editors.getSelectedFiles()));
        assertEquals("the mark moved the caret", caretBefore, myFixture.getEditor().getCaretModel().getOffset());
    }

    // Rule-CODEGEN-030
    public void testTheJumpWaitsForIndexingToFinish() {
        final @NotNull TestCase testCase = indexedTestCase(indexedTestSet("Login", theTestCasesDirectory()), "Log in", "b");
        final @NotNull GutterMark mark = theMarkBeside(testCase);
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String wasRoot = settings.rootTestinPath;
        final @NotNull TestProjectIndexer indexer = Services.getInstance(getProject(), TestProjectIndexer.class);
        settings.rootTestinPath = root.toString();
        try {
            indexer.resetForReindex();
            assertTrue("the test case is still known before indexing, so waiting proves nothing", Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getUuid()).isEmpty());

            clicked(mark);

            Await.until("the jump did not wait for indexing, and found no test case to open", () -> showing(testCase));
        } finally {
            settings.rootTestinPath = wasRoot;
        }
    }
}
