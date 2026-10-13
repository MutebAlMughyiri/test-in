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

package org.testin.explorer;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.StatusText;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.actions.TestinDataKeys;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WithoutGitIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String NEEDS_GIT = "(needs the Git plugin)";

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();

    private String wasBound;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
        wasBound = bound().name();
        bound().choose("NAFATH");
        settings().rootTestinPath = root.toString();
        OptionalPlugin.GIT.missingUntil(getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        try {
            XmlSerializerUtil.copyBean(wasStored, settings());
            bound().choose(wasBound);
            Services.getInstance(getProject(), ProjectIndexer.class).resetForReindex();
        } finally {
            super.tearDown();
        }
    }

    // Rule-PRODUCT-021
    public void testWithoutGitThereIsNoSyncAndNoCloneAndTheDataIsStillUsable() {
        final @NotNull NodesOnDisk onDisk = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = onDisk.testProject(root.resolve("NAFATH"));
        final @NotNull TestCase testCase = onDisk.testCase(onDisk.testSet(testProject.getTestCasesFolder(), "Login"));

        final @NotNull AnAction sync = Optional.ofNullable(ActionManager.getInstance().getAction("Testin.SyncWithRemote")).orElseThrow(() -> new AssertionError("Sync is not registered"));
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(sync, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_NODES, List.of(testProject))
                .build());
        ActionUtil.updateAction(sync, e);
        assertFalse("Sync is offered without Git", e.getPresentation().isEnabled());
        assertTrue("the gray Sync does not say it needs Git: " + e.getPresentation().getText(), String.valueOf(e.getPresentation().getText()).endsWith(NEEDS_GIT));

        final @NotNull TreePanel treePanel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), treePanel);
        final @NotNull StatusText offered = new JBList<String>().getEmptyText();
        treePanel.offerClone(offered);
        final @NotNull String lines = StreamSupport.stream(offered.getWrappedFragmentsIterable().spliterator(), false)
                .filter(SimpleColoredComponent.class::isInstance)
                .map(line -> ((SimpleColoredComponent) line).getCharSequence(false).toString())
                .collect(Collectors.joining("\n"));
        assertTrue("cloning is offered without Git: " + lines, lines.contains(NEEDS_GIT));

        final @NotNull ProjectIndexer indexer = Services.getInstance(getProject(), ProjectIndexer.class);
        indexer.resetForReindex();
        ApplicationManager.getApplication().executeOnPooledThread(indexer::awaitIndexing);
        Await.until("without Git the test cases on disk are not read", () -> Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getId()).isPresent());
    }
}
