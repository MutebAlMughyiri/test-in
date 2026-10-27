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

package org.testin.search;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.TestActionEvent;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.open.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValues;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Icons;

import java.util.List;
import java.util.Optional;

public class GoToIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode checkout = new TestSetNode();

    private @NotNull List<TestCase> testCases = List.of();

    @Override
    protected void setUp() {
        super.setUp();
        checkout = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        testCases = EditorFixtures.testCases(getProject(), checkout, 3);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), GlobalSearchDialog.class);
        editors().closeAll();
        super.tearDown();
    }

    private @NotNull TestinEditors editors() {
        return Services.getInstance(getProject(), TestinEditors.class);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002, Rule-VIEW-PANEL-010
    public void testChoosingATestCaseOpensItsTestSetWithTheTestCaseSelected() {
        final @NotNull TestCase second = testCases.get(1);

        GoTo.the(getProject(), Hit.of(second));

        Await.until("choosing a test case did not open its test set with it selected", () -> editors().editorFor(checkout).map(editor -> editor.getSelectedTestCases().stream().map(TestCase::getUuid).toList()).equals(Optional.of(List.of(second.getUuid()))));
    }

    // UC-INTERNAL-001
    public void testSearchOpensTheSearch() {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(GlobalSearchAction.registered(), SimpleDataContext.getProjectContext(getProject()));
        ActionUtil.updateAction(GlobalSearchAction.registered(), e);
        assertTrue("Search is gray: " + e.getPresentation().getDescription(), e.getPresentation().isEnabled());

        ActionUtil.performAction(GlobalSearchAction.registered(), e);

        ShownDialog.waitedFor(getProject(), GlobalSearchDialog.class);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    public void testChoosingATestSetTakesTheTreeToIt() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String rootWas = settings.rootTestinPath;
        final @NotNull ToolWindow window = ToolWindowManager.getInstance(getProject()).registerToolWindow("testin.tree", _ -> Unit.INSTANCE);
        try {
            settings.rootTestinPath = root.toString();
            Services.getInstance(getProject(), BoundTestProject.class).choose("NAFATH");
            final @NotNull TreePanel panel = Services.getInstance(getProject(), TreePanel.class);
            panel.reindex();
            Await.until("the panel never showed the tree", panel::showsTree);

            GoTo.the(getProject(), new Hit(Icons.TEST_CASE, checkout.getName(), "", checkout, Optional.empty()));

            Await.until("choosing a test set did not take the tree to it", () -> Optional.ofNullable(panel.getTestProjectTree().getMainTree().getSelectionPath()).flatMap(TreeValues::directoryAt).map(AbstractNode::getPath).filter(checkout.getPath()::equals).isPresent());
        } finally {
            window.remove();
            Services.getInstance(getProject(), BoundTestProject.class).choose("");
            settings.rootTestinPath = rootWas;
        }
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    public void testChoosingATestSetOpensIt() {
        GoTo.the(getProject(), new Hit(Icons.TEST_CASE, checkout.getName(), "", checkout, Optional.empty()));

        Await.until("choosing a test set did not open it", () -> editors().editorFor(checkout).isPresent());
    }
}
