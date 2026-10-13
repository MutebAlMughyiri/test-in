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

package org.testin.importexport.imports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.codegen.JavaSourceRoot;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class SheetCodeUndoIdeTest extends AbstractCodegenIdeTest {

    private static final int SHEET = 450;

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    // Rule-CODEGEN-018
    public void testAWholeSheetIsWrittenInStepsOf200AndCtrlZTakesItAllBack() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCase> sheet = IntStream.range(0, SHEET)
                .mapToObj(i -> indexedTestCase(login, "Log in as user " + i, String.format(Locale.ROOT, "b%04d", i)))
                .toList();
        final @NotNull VirtualFile loginTest = Optional.ofNullable(JavaSourceRoot.find(getProject()).orElseThrow().findFileByRelativePath("nafath/LoginTest.java")).orElseThrow(() -> new AssertionError("the test set's class was never written"));
        myFixture.openFileInEditor(loginTest);

        final @NotNull List<String> steps = new ArrayList<>();
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable()).subscribe(CommandListener.TOPIC, new CommandListener() {
            @Override
            public void commandStarted(final @NotNull CommandEvent event) {
                steps.add(String.valueOf(event.getCommandGroupId()));
            }
        });

        final @NotNull Future<?> imported = ApplicationManager.getApplication().executeOnPooledThread(() -> new ImportWork(getProject()).generateTestMethods(sheet, login.getName(), new EmptyProgressIndicator()));
        Await.until("the sheet's code was never written", imported::isDone);
        settled();

        assertEquals("the sheet was not written in steps of 200 test cases: " + steps, 3, steps.size());
        assertEquals("the steps are not one change on the undo history: " + steps, 1, new HashSet<>(steps).size());
        assertEquals("not every test case on the sheet got its method", SHEET, generatedClass(LOGIN_TEST).orElseThrow().getMethods().length);

        final @NotNull TextEditor editor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        UndoManager.getInstance(getProject()).undo(editor);
        settled();

        assertEquals("Ctrl+Z took back only part of the sheet's code", 0, generatedClass(LOGIN_TEST).orElseThrow().getMethods().length);
    }
}
