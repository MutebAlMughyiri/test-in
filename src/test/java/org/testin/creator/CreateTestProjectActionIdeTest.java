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

package org.testin.creator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.creator.dialogs.CreateTestProjectDialog;
import org.testin.explorer.TreePanel;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testin.git.LocalGit.mustGit;

public class CreateTestProjectActionIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull Path elsewhere = Path.of("");

    @Override
    protected void setUp() {
        super.setUp();
        try {
            elsewhere = Files.createTempDirectory("testin-new-test-project");
        } catch (final IOException ex) {
            throw new AssertionError("could not make a folder for the repository to clone: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), CreateTestProjectDialog.class);
        FileUtil.delete(elsewhere.toFile());
        super.tearDown();
    }

    private @NotNull CreateTestProjectAction newTestProject() {
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
        return new CreateTestProjectAction(getProject(), panel);
    }

    private @NotNull AnActionEvent pressed(final @NotNull CreateTestProjectAction action) {
        return TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject()));
    }

    private void typedAndConfirmed(final @NotNull String typed) {
        final @NotNull CreateTestProjectAction action = newTestProject();
        ActionUtil.performAction(action, pressed(action));
        ShownDialog.waitedFor(getProject(), CreateTestProjectDialog.class);
        ShownDialog.typed(getProject(), CreateTestProjectDialog.class, typed);
        ShownDialog.press(getProject(), CreateTestProjectDialog.class, Shortcuts.Enter);
    }

    private @NotNull String aBareRepositoryHoldingATestProject() {
        final @NotNull Path work = aTestProjectAt(elsewhere.resolve("work"));
        aTestCaseIn(theTestCasesOf(work));
        mustGit(work, "init", "--initial-branch=main");
        mustGit(work, "config", "user.name", "Colleague");
        mustGit(work, "config", "user.email", "colleague@example.invalid");
        mustGit(work, "add", "-A");
        mustGit(work, "commit", "-m", "the test project");
        final @NotNull Path bare = elsewhere.resolve("checkout-test-cases.git");
        mustGit(elsewhere, "clone", "--bare", work.toString(), bare.toString());
        return bare.toUri().toString().replaceFirst("/$", "");
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    // UC-TREE-PANEL-002
    public void testANameTypedMakesTheTestProjectAndChoosesIt() {
        readEverything();

        typedAndConfirmed("Mobile");

        Await.until("the test project typed was not made and chosen", () -> "Mobile".equals(bound().name()));
        assertTrue("the test project typed has no folder in the Testin folder", Files.isDirectory(root.resolve("Mobile")));
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-107
    public void testARepositoryAddressTypedIsClonedUnderTheRepositoryName() {
        readEverything();

        typedAndConfirmed(aBareRepositoryHoldingATestProject());

        Await.until("the repository typed was not cloned and chosen under its name", () -> "CHECKOUT".equals(bound().name()));
        assertTrue("the clone did not land in the Testin folder", Files.isDirectory(root.resolve("CHECKOUT").resolve(".git")));
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-115
    public void testNewTestProjectIsGrayUntilATestinFolderIsSetAndSaysWhy() {
        final @NotNull CreateTestProjectAction action = newTestProject();
        settings().rootTestinPath = "";
        final @NotNull AnActionEvent withoutARoot = pressed(action);

        ActionUtil.updateAction(action, withoutARoot);

        assertFalse("New Test Project is offered with no Testin folder set", withoutARoot.getPresentation().isEnabled());
        assertEquals("the gray New Test Project does not say why", Bundle.message("toolbar.disabled.no.root"), withoutARoot.getPresentation().getDescription());
    }
}
