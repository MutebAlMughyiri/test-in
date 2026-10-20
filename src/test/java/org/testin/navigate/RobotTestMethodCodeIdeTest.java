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

package org.testin.navigate;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RobotTestMethodCodeIdeTest extends AbstractCodegenIdeTest {

    private boolean caretAt(final @NotNull String fileName, final int offset) {
        final @NotNull FileEditorManager editors = FileEditorManager.getInstance(getProject());
        return Arrays.stream(editors.getSelectedFiles()).anyMatch(file -> file.getName().equals(fileName))
                && Optional.ofNullable(editors.getSelectedTextEditor()).map(Editor::getCaretModel).filter(caret -> caret.getOffset() == offset).isPresent();
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-119
    public void testARobotTestIsFoundByTheTestCaseUuidThatIsItsFirstTagWhereverItWasMoved() {
        interpreterIs("Python SDK");
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase untouched = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase written = createdTestCase(login, "Refuse a wrong password", "b");
        final @NotNull TestCase withoutTest = createdTestCase(login, "Lock the account", "c");
        final @NotNull String suite = """
                *** Test Cases ***
                Sign in
                    [Tags]    %s    SMOKE
                    # TODO: Auto-generated test steps for Sign in
                    No Operation
                
                Refuse a wrong password
                    [Tags]    %s
                    Open Browser    https://example.com/login    chrome
                """.formatted(untouched.getUuid(), written.getUuid());
        myFixture.addFileToProject("moved/by/hand/Elsewhere.robot", suite);

        assertEquals("the Robot tests were not found by their first tag, or their bodies were misread",
                Map.of(untouched.getUuid(), false, written.getUuid(), true),
                TestMethodCode.available(getProject()).methodsFor(getProject(), List.of(untouched, written, withoutTest)));

        TestMethodCode.available(getProject()).toCode(getProject(), written);
        Await.until("Navigate to Test Method did not open the Robot test at its name line", () -> caretAt("Elsewhere.robot", suite.indexOf("Refuse a wrong password")));
    }
}
