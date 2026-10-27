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

package org.testin.testproject;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.explorer.TreePanel;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JTable;
import java.util.List;
import java.util.stream.IntStream;

public class SelectTestProjectActionIdeTest extends AbstractReadTheRootIdeTest {

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), BindTestProjectDialog.class);
        super.tearDown();
    }

    private @NotNull SelectTestProjectAction select() {
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
        return new SelectTestProjectAction(getProject(), panel);
    }

    private @NotNull AnActionEvent pressed(final @NotNull SelectTestProjectAction action) {
        return TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject()));
    }

    // UC-TREE-PANEL-004
    public void testATestinFolderWithNoTestProjectHasNothingToChooseAndSaysSo() {
        readEverything();
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull SelectTestProjectAction select = select();

        ActionUtil.performAction(select, pressed(select));

        Await.until("an empty Testin folder did not say there is nothing to choose", () -> said.shown().contains(Bundle.message("test.project.none.title") + "\n" + Bundle.message("test.project.none.message")));
        assertFalse("the choice opened with nothing in it", ShownDialog.isOpen(getProject(), BindTestProjectDialog.class));
    }

    // UC-TREE-PANEL-004
    public void testTheTestProjectsInTheTestinFolderAreOfferedToChooseFrom() {
        aTestProjectAt(root.resolve("CHECKOUT"));
        aTestProjectAt(root.resolve("PAYMENTS"));
        readEverything();
        final @NotNull SelectTestProjectAction select = select();

        ActionUtil.performAction(select, pressed(select));

        final @NotNull JTable offered = Drawn.first(ShownDialog.waitedFor(getProject(), BindTestProjectDialog.class), JTable.class);
        assertEquals("the test projects in the Testin folder are not the ones offered", List.of("CHECKOUT", "PAYMENTS"), IntStream.range(0, offered.getRowCount()).mapToObj(row -> String.valueOf(offered.getValueAt(row, 0))).sorted().toList());
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-115
    public void testSelectIsGrayUntilATestinFolderIsSetAndSaysWhy() {
        final @NotNull SelectTestProjectAction select = select();
        final @NotNull AnActionEvent withARoot = pressed(select);
        select.update(withARoot);
        assertTrue("Select is gray with a Testin folder set", withARoot.getPresentation().isEnabled());

        settings().rootTestinPath = "";
        final @NotNull AnActionEvent withoutARoot = pressed(select);
        select.update(withoutARoot);

        assertFalse("Select is offered with no Testin folder set", withoutARoot.getPresentation().isEnabled());
        assertEquals("the gray Select does not say why", Bundle.message("toolbar.disabled.no.root"), withoutARoot.getPresentation().getDescription());
    }
}
