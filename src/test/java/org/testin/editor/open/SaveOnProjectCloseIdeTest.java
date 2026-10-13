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

package org.testin.editor.open;

import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import java.util.Objects;

public class SaveOnProjectCloseIdeTest extends AbstractTempRootIdeTest {

    // Rule-EDITOR-PANEL-015
    public void testClosingTheCodeProjectRemembersEveryOpenEditorAndClosesItsTab() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 1);
        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        try {
            editors.open(testSet, false);
            Await.until("the test set never opened in an editor", () -> !editors.openNodePaths().isEmpty());

            new SaveOnProjectClose().projectClosingBeforeSave(getProject());

            assertTrue("the editor's tab was left open after the code project closed", editors.openNodePaths().isEmpty());
            assertTrue("the open editor was not remembered for next time", Objects.toString(PropertiesComponent.getInstance(getProject()).getValue("testin.openEditors"), "").contains(testSet.getPath().toAbsolutePath().toString()));
        } finally {
            editors.closeAll();
            PropertiesComponent.getInstance(getProject()).unsetValue("testin.openEditors");
        }
    }
}
