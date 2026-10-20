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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.util.Bundle;

public class RobotGutterIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull ExtensionFileNameMatcher ROBOT_FILES = new ExtensionFileNameMatcher("robot");

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029, Rule-CODEGEN-118
    public void testTheMarkIsDrawnInASuiteFileOpenedAsPlainTextWithNoRobotPlugin() {
        WriteAction.runAndWait(() -> FileTypeManager.getInstance().associate(PlainTextFileType.INSTANCE, ROBOT_FILES));
        Disposer.register(getTestRootDisposable(), () -> WriteAction.runAndWait(() -> FileTypeManager.getInstance().removeAssociation(PlainTextFileType.INSTANCE, ROBOT_FILES)));

        myFixture.configureByText("Login.robot", """
                *** Test Cases ***
                Sign in
                    [Tags]    11111111-1111-4111-8111-111111111101    smoke
                    No Operation
                
                Written by hand
                    [Tags]    smoke
                    No Operation
                """);

        assertEquals("a Robot test whose first tag is a test case UUID, in a file the IDE reads as plain text, has no mark, or a test without one has one",
                1, myFixture.findAllGutters().stream().filter(mark -> Bundle.message("gutter.view.details").equals(mark.getTooltipText())).count());
    }
}
