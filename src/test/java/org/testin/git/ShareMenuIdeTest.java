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

package org.testin.git;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.tree.TreeContextMenu;
import org.testin.services.OptionalPlugin;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.List;

public class ShareMenuIdeTest extends BasePlatformTestCase {
    private static final @NotNull List<String> SHARE_ACTIONS = List.of("Testin.Export", "Testin.Import", "Testin.SyncWithRemote", "Testin.ViewPendingCommits");

    private static @NotNull AnAction action(final @NotNull String id) {
        final @NotNull AnAction action = ActionManager.getInstance().getAction(id);
        assertNotNull(id + " is not declared", action);
        return action;
    }

    private @NotNull AnActionEvent updated(final @NotNull String id) {
        final @NotNull AnActionEvent event = TestActionEvent.createTestEvent(action(id), SimpleDataContext.getProjectContext(getProject()));
        ActionUtil.updateAction(action(id), event);
        return event;
    }

    // UC-SHARE-001, Rule-SHARE-006
    public void testNothingHereIsInTheKeymap() {
        for (final String id : SHARE_ACTIONS) {
            action(id);
            for (final Keymap keymap : KeymapManagerEx.getInstanceEx().getAllKeymaps()) {
                assertEquals(id + " has a key in the " + keymap.getName() + " keymap", 0, keymap.getShortcuts(id).length);
            }
        }
    }

    // UC-SHARE-010, Rule-SHARE-105
    public void testWithoutTheGitPluginBothGitEntriesStayInTheMenuGrayedWithWhatTheyNeed() {
        OptionalPlugin.GIT.missingUntil(getTestRootDisposable());
        final @NotNull List<AnAction> menu = Arrays.asList(new TreeContextMenu(getProject(), new SimpleTree()).getChildActionsOrStubs());

        for (final String id : List.of("Testin.SyncWithRemote", "Testin.ViewPendingCommits")) {
            assertTrue(id + " was left out of the menu", menu.contains(action(id)));

            final @NotNull AnActionEvent event = updated(id);
            assertTrue(id + " was hidden", event.getPresentation().isVisible());
            assertFalse(id + " can be pressed without Git", event.getPresentation().isEnabled());
            assertEquals(Bundle.message("plugin.needs", Bundle.message("action." + id + ".text"), "Git"), event.getPresentation().getText());
        }
    }
}
