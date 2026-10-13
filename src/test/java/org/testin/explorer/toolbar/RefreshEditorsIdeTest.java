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

package org.testin.explorer.toolbar;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TempTree;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.TestinEditor;
import org.testin.editor.open.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;

import javax.swing.JTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class RefreshEditorsIdeTest extends AbstractOpenEditorsIdeTest {

    private @NotNull String rootWas = "";
    private TestSetNode login;
    private TestSetNode checkout;
    private TestSetNode card;
    private TestCase inCheckout;
    private TestCase inCard;
    private TreePanel panel;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static void aSecondTestCaseWrittenBesides(final @NotNull TestSetNode testSet, final @NotNull TestCase first) {
        final @NotNull Path written = testSet.getPath().resolve(FileKind.TEST_CASE.fileName(first.getId()));
        final @NotNull UUID second = UUID.randomUUID();
        try {
            Files.writeString(testSet.getPath().resolve(FileKind.TEST_CASE.fileName(second)), Files.readString(written).replace(first.getId().toString(), second.toString()).replace("\"m\"", "\"n\""));
        } catch (final IOException ex) {
            throw new AssertionError("could not write a second test case into " + testSet.getName() + ": " + ex.getMessage(), ex);
        }
    }

    private static void aCellIsBeingEditedIn(final @NotNull TestinEditor editor) {
        ((AbstractTestinEditor<?, ?>) editor).onToolBarSwitchedToGridView();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JTable grid = Optional.ofNullable(UIUtil.uiTraverser(editor.getComponent()).filter(JTable.class).first()).orElseThrow(() -> new AssertionError("the editor shows no grid"));
        for (int column = 0; column < grid.getColumnCount() && !grid.isEditing(); column++) grid.editCellAt(0, column);
        assertTrue("no cell could be opened for editing", editor.isBusy());
    }

    @Override
    public void setUp() {
        super.setUp();
        rootWas = settings().rootTestinPath;
        settings().rootTestinPath = root.toString();

        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        checkout = made.testSet(testProject.getTestCasesFolder(), "Checkout");
        card = made.testSet(testProject.getTestCasesFolder(), "Card");
        inCheckout = made.testCase(checkout);
        inCard = made.testCase(card);
        Services.getInstance(getProject(), BoundTestProject.class).choose("NAFATH");

        panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
    }

    @Override
    public void tearDown() {
        Services.getInstance(getProject(), BoundTestProject.class).choose("");
        settings().rootTestinPath = rootWas;
        super.tearDown();
    }

    private @NotNull TestinEditor editorOn(final @NotNull TestSetNode testSet) {
        final @NotNull TestinEditor editor = Services.getInstance(getProject(), TestinEditors.class).editorFor(testSet).orElseThrow(() -> new AssertionError(testSet.getName() + " has no editor open"));
        Await.until("the editor on " + testSet.getName() + " never loaded", () -> !editor.isLoading());
        return editor;
    }

    // Rule-TREE-PANEL-082
    public void testRefreshClosesTheEditorOnAGoneNodeReloadsTheRestAndLeavesABusyOneAlone() {
        opened(login);
        opened(checkout);
        opened(card);
        final @NotNull TestinEditor checkoutEditor = editorOn(checkout);
        final @NotNull TestinEditor cardEditor = editorOn(card);
        assertEquals(1, checkoutEditor.getTotalTestCaseCount());

        TempTree.delete(login.getPath());
        aSecondTestCaseWrittenBesides(checkout, inCheckout);
        aSecondTestCaseWrittenBesides(card, inCard);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        aCellIsBeingEditedIn(cardEditor);

        panel.getRefreshAction().execute();

        Await.until("the editor on a node that no longer exists is still open", () -> openOn(login).isEmpty());
        Await.until("the editor on a node that still exists was not reloaded", () -> checkoutEditor.getTotalTestCaseCount() == 2);
        assertEquals("the editor on a node that still exists was closed", 1, openOn(checkout).size());
        assertTrue("an editor the tester is in the middle of was reloaded under them", cardEditor.isBusy());
    }
}
