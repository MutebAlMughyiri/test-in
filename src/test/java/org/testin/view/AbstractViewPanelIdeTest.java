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
package org.testin.view;

import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.util.List;

public abstract class AbstractViewPanelIdeTest extends AbstractTempRootIdeTest {
    protected ViewOnScreen view;
    protected TestProjectNode testProject;
    private String rootBefore = "";

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        rootBefore = settings().rootTestinPath;
        settings().rootTestinPath = root.toString();
        testProject = EditorFixtures.testProject(getProject(), root);
        view = ViewOnScreen.closed(getProject(), getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        settings().rootTestinPath = rootBefore;
        super.tearDown();
    }

    protected @NotNull TestSetNode aTestSet(final @NotNull String name) {
        return EditorFixtures.testSet(getProject(), testProject, name);
    }

    protected @NotNull TestCase aTestCase(final @NotNull TestSetNode testSet, final @NotNull String description, final @NotNull String order) {
        return EditorFixtures.testCase(getProject(), testSet, description, order);
    }

    protected @NotNull TestRunNode aTestRun(final @NotNull List<RunItem> runItems) {
        return EditorFixtures.testRun(getProject(), testProject, runItems);
    }

    protected @NotNull List<String> details() {
        return view.words(ViewTab.DETAILS);
    }

    protected void settled() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }
}
