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

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.notifications.Refused;
import org.testin.util.Bundle;

import java.util.List;

public class RobotGutterIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull ExtensionFileNameMatcher ROBOT_FILES = new ExtensionFileNameMatcher("robot");

    private void robotFilesReadAsPlainText() {
        WriteAction.runAndWait(() -> FileTypeManager.getInstance().associate(PlainTextFileType.INSTANCE, ROBOT_FILES));
        Disposer.register(getTestRootDisposable(), () -> WriteAction.runAndWait(() -> FileTypeManager.getInstance().removeAssociation(PlainTextFileType.INSTANCE, ROBOT_FILES)));
    }

    private @NotNull List<GutterMark> detailsMarks() {
        return myFixture.findAllGutters().stream().filter(mark -> Bundle.message("gutter.view.details").equals(mark.getTooltipText())).toList();
    }

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029, Rule-CODEGEN-118
    public void testTheMarkIsDrawnInASuiteFileOpenedAsPlainTextWithNoRobotPlugin() {
        robotFilesReadAsPlainText();

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
                1, detailsMarks().size());
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069, Rule-CODEGEN-118
    public void testClickingTheMarkOfARobotTestWhoseTestCaseIsGoneSaysSoNamingTheTest() {
        robotFilesReadAsPlainText();
        myFixture.configureByText("Login.robot", """
                *** Test Cases ***
                Sign in
                    [Tags]    11111111-1111-4111-8111-111111111101
                    No Operation
                """);
        final @NotNull List<GutterMark> marks = detailsMarks();
        assertEquals("a Robot test whose test case is gone has no mark to click", 1, marks.size());
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        clicked(marks.getFirst());

        final @NotNull String gone = Refused.NO_TEST_CASE_BEHIND_IT.about("Sign in");
        Await.until("clicking the mark of a Robot test whose test case is gone did not name the test: " + balloons.shown(), () -> balloons.shown().contains(gone));
    }
}
