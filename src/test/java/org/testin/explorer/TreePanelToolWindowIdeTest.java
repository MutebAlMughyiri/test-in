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

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.content.Content;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.services.Services;

import java.util.Arrays;
import java.util.List;

public class TreePanelToolWindowIdeTest extends AbstractIdeTest {

    private static final @NotNull String WINDOW = "Testin tree panel under test";

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-097
    public void testTheToolWindowHoldsTheProjectsOneTreePanel() {
        final @NotNull ToolWindow window = ToolWindowManager.getInstance(getProject()).registerToolWindow(WINDOW, _ -> Unit.INSTANCE);
        try {
            new TreePanelToolWindowFactory().createToolWindowContent(getProject(), window);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("the tool window does not hold the project's one tree panel", List.of(Services.getInstance(getProject(), TreePanel.class).getPanel()),
                    Arrays.stream(window.getContentManager().getContents()).map(Content::getComponent).toList());
        } finally {
            window.remove();
        }
    }
}
