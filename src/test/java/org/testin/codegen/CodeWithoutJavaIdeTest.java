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

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.TestinLog;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.card.Offered;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.view.details.TestCaseHoverButtons;

import javax.swing.Icon;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertNotEquals;

public class CodeWithoutJavaIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String NEEDS_JAVA = "(needs the Java plugin)";

    private static final float DRAWN_AT = 1.3f;

    private static @NotNull Icon iconOf(final @NotNull JBLabel label) {
        return Optional.ofNullable(label.getIcon()).orElseThrow(() -> new AssertionError("the class button has no icon"));
    }

    private @NotNull TestCaseDto aTestCase() {
        return indexedTestCase(indexedTestSet("Login", theTestCasesDirectory()), "Log in with a valid user", "b");
    }

    private @NotNull VirtualFile theTestSourceRoot() {
        return JavaSourceRoot.find(getProject()).orElseThrow(() -> new AssertionError("the project has no test source folder"));
    }

    private @NotNull AnActionEvent theMenuEntryFor(final @NotNull TestCaseDto testCase) {
        final @NotNull AnAction action = Optional.ofNullable(ActionManager.getInstance().getAction("Testin.NavigateToTestMethod")).orElseThrow(() -> new AssertionError("Testin.NavigateToTestMethod is not registered"));
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, List.of(testCase))
                .build());
        ActionUtil.updateAction(action, e);
        return e;
    }

    private @NotNull JBLabel theClassButtonOnTheViewPanel(final @NotNull TestCaseDto testCase) {
        return (JBLabel) TestCaseHoverButtons.of(getProject(), testCase, Optional.empty()).stream()
                .filter(JBLabel.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the view panel left the class button out"));
    }

    private @NotNull String grayed(final @NotNull TestCaseDto testCase, final @NotNull JBLabel label) {
        final @NotNull Icon drawn = Services.getInstance(getProject(), AutomationState.class).of(testCase.getId()).getIcon();
        return String.valueOf(IconUtil.scale(IconLoader.getDisabledIcon(drawn), label, DRAWN_AT));
    }

    // Rule-CODEGEN-005
    public void testWithoutTheJavaCodeGeneratorsTestManagementGoesOnAndNoCodeIsWritten() {
        ExtensionTestUtil.maskExtensions(CodeChangeRegistry.EP, List.of(), getTestRootDisposable());

        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto testCase = createdTestCase(login, "Log in with a valid user", "b");
        testCase.setDescription("Sign in with a valid user");
        GenType.UPDATE_TEST_CASE_DESCRIPTION.execute(getProject(), testCase);
        GenType.REMOVE_TEST_CASE.execute(getProject(), testCase);
        GenType.REMOVE_TEST_SET.execute(getProject(), login);
        settled();

        assertNull("code was written without the Java code generators", theTestSourceRoot().findFileByRelativePath("nafath/LoginTest.java"));
        assertTrue("the test case was lost because its code could not be written", Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getId()).isPresent());
    }

    // Rule-CODEGEN-006
    public void testWhatGoesWrongWritingCodeGoesToTheLogAndTheTesterIsNotShownIt() {
        final @NotNull Said heard = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull TestinLog log = TestinLog.fromNow(getTestRootDisposable());
        WriteAction.runAndWait(() -> {
            try {
                theTestSourceRoot().createChildData(this, "nafath");
            } catch (final IOException ex) {
                throw new AssertionError("could not put a file where the package folder goes: " + ex.getMessage(), ex);
            }
        });
        final @NotNull TestSetNode login = createdTestSet("Login");
        createdTestCase(login, "Log in with a valid user", "b");
        settled();

        assertTrue("a file standing where the package folder goes did not stop the class", generatedClass("nafath.LoginTest").isEmpty());
        assertTrue("what went wrong was shown to the tester: " + heard.notifications().stream().map(Notification::getContent).toList(), heard.notifications().isEmpty());
        assertTrue("what went wrong was shown to the tester in a balloon: " + heard.shown(), heard.shown().isEmpty());
        assertTrue("what went wrong never reached the log", log.lines().stream().anyMatch(line -> line.contains("[ERROR]") && line.contains("nafath")));
    }

    // Rule-CODEGEN-027
    public void testWithoutTheJavaPluginTheClassButtonIsGrayAndTheMenuEntrySaysWhy() {
        final @NotNull TestCaseDto testCase = aTestCase();
        final @NotNull JBLabel working = theClassButtonOnTheViewPanel(testCase);
        assertNotEquals("the class button is gray even with the Java plugin, so gray proves nothing", grayed(testCase, working), String.valueOf(iconOf(working)));

        OptionalPlugin.JAVA.missingUntil(getTestRootDisposable());
        final @NotNull Offered onCard = TestCaseQuickAction.onCard(getProject(), testCase.getParent(), testCase).getFirst();
        assertEquals("the card left the class button out", TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD, onCard.action());
        assertFalse("the class button on the card is drawn live without the Java plugin", onCard.works());
        assertTrue("the class button on the card does not say it needs the Java plugin", onCard.hintText().endsWith(NEEDS_JAVA));

        final @NotNull JBLabel classIcon = theClassButtonOnTheViewPanel(testCase);
        assertEquals("the class button on the view panel is not drawn gray", grayed(testCase, classIcon), String.valueOf(iconOf(classIcon)));
        assertTrue("the gray class button on the view panel does not say it needs the Java plugin", String.valueOf(classIcon.getAccessibleContext().getAccessibleDescription()).endsWith(NEEDS_JAVA));
        final int width = iconOf(classIcon).getIconWidth();
        Arrays.stream(classIcon.getMouseListeners()).forEach(listener -> listener.mouseEntered(new MouseEvent(classIcon, MouseEvent.MOUSE_ENTERED, 0, 0, 1, 1, 0, false)));
        assertEquals("the gray class button on the view panel grows under the pointer", width, iconOf(classIcon).getIconWidth());
        assertEquals("the gray class button on the view panel offers a hand", Cursor.DEFAULT_CURSOR, classIcon.getCursor().getType());

        final @NotNull AnActionEvent menu = theMenuEntryFor(testCase);
        assertTrue("the menu entry was left out", menu.getPresentation().isVisible());
        assertFalse("the menu entry is live without the Java plugin", menu.getPresentation().isEnabled());
        assertTrue("the gray menu entry does not read (needs the Java plugin): " + menu.getPresentation().getText(), String.valueOf(menu.getPresentation().getText()).endsWith(NEEDS_JAVA));
    }

    // Rule-CODEGEN-063
    public void testAPluginTestinFoundMissingStaysMissingUntilTheIdeRestarts() {
        final @NotNull PluginId java = PluginId.getId("com.intellij.java");
        assertTrue("the Java plugin is not installed and switched on here, so this proves nothing", PluginManagerCore.isPluginInstalled(java) && !PluginManagerCore.isDisabled(java));

        OptionalPlugin.JAVA.missingUntil(getTestRootDisposable());
        assertFalse("Testin noticed the Java plugin switched on without the IDE restarting", OptionalPlugin.JAVA.isAvailable());
        assertFalse("automation code is on while Testin counts the Java plugin missing", CodeOn.isOn(getProject()));

        createdTestSet("Login");
        settled();
        assertNull("a class was written while Testin counts the Java plugin missing", theTestSourceRoot().findFileByRelativePath("nafath/LoginTest.java"));
    }
}
