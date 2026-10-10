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

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.testFramework.DumbModeTestUtils;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.LoginTestSource;
import org.testin.Said;
import org.testin.notifications.Refused;
import org.testin.util.Bundle;

import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestCaseMarkIdeTest extends AbstractCodegenIdeTest {

    private static @NotNull String aTestMethod(final @NotNull String testName) {
        return LoginTestSource.withTestNg(testName);
    }

    private static void clicked(final @NotNull GutterMark mark) {
        clicked(((LineMarkerInfo.LineMarkerGutterIconRenderer<?>) mark).getLineMarkerInfo());
    }

    private static <T extends PsiElement> void clicked(final @NotNull LineMarkerInfo<T> info) {
        final @NotNull T element = Optional.ofNullable(info.getElement()).orElseThrow(() -> new AssertionError("the mark stands beside nothing"));
        Optional.ofNullable(info.getNavigationHandler()).orElseThrow(() -> new AssertionError("the mark does nothing when clicked"))
                .navigate(new MouseEvent(new JPanel(), MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false), element);
    }

    private @NotNull List<GutterMark> marksBeside(final @NotNull String testName) {
        final @NotNull String text = aTestMethod(testName);
        final @NotNull PsiFile file = myFixture.addFileToProject("nafath/LoginTest.java", text);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.getEditor().getCaretModel().moveToOffset(text.indexOf(testName));

        return myFixture.findGuttersAtCaret().stream()
                .filter(mark -> Bundle.message("gutter.view.details").equals(mark.getTooltipText()))
                .toList();
    }

    // Rule-CODEGEN-030
    public void testTheMarkIsDrawnWhileTheIdeIsStillIndexing() {
        final @NotNull String testName = UUID.randomUUID().toString();
        final @NotNull PsiFile file = myFixture.addFileToProject("nafath/LoginTest.java", aTestMethod(testName));
        final @NotNull LineMarkerProvider gutter = LineMarkerProviders.getInstance().allForLanguage(JavaLanguage.INSTANCE).stream()
                .filter(provider -> provider.getClass().getSimpleName().equals("TestMethodGutter"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Testin's gutter is not registered for Java"));
        assertTrue("the gutter is put aside while the IDE indexes", DumbService.isDumbAware(gutter));

        final @NotNull List<LineMarkerInfo<?>> marks = new ArrayList<>();
        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue("the IDE is not indexing, so this proves nothing", DumbService.isDumb(getProject()));
            try {
                gutter.collectSlowLineMarkers(SyntaxTraverser.psiTraverser(file).toList(), marks);
            } catch (final IndexNotReadyException ex) {
                throw new AssertionError("the mark asks the index which annotation it stands in, so it cannot be drawn while the IDE is indexing", ex);
            }
        });

        assertEquals("the mark is not drawn while the IDE is indexing", 1, marks.stream().filter(mark -> Bundle.message("gutter.view.details").equals(mark.getLineMarkerTooltip())).count());
    }

    // Rule-CODEGEN-069
    public void testClickingTheMarkOfAMethodWhoseTestCaseIsGoneSaysSoNamingTheMethod() {
        final @NotNull List<GutterMark> marks = marksBeside(UUID.randomUUID().toString());
        assertEquals("a method whose test case is gone has no mark to click", 1, marks.size());
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        clicked(marks.getFirst());

        final @NotNull String gone = Refused.NO_TEST_CASE_BEHIND_IT.about("logIn");
        Await.until("clicking the mark of a method whose test case is gone said nothing", () -> balloons.shown().contains(gone));
    }
}
