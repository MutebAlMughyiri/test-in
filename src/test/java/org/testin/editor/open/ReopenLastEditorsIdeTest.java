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
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import java.util.Objects;
import java.util.Set;

public class ReopenLastEditorsIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String OPEN_EDITORS = "testin.openEditors";

    @Override
    protected void tearDown() {
        Services.getInstance(getProject(), TestinEditors.class).closeAll();
        PropertiesComponent.getInstance(getProject()).unsetValue(OPEN_EDITORS);
        super.tearDown();
    }

    private @NotNull TestSetNode aTestSetIn(final @NotNull TestProjectNode testProject, final @NotNull String name) {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, name);
        EditorFixtures.testCases(getProject(), testSet, 1);
        return testSet;
    }

    // UC-INTERNAL-002, Rule-EDITOR-PANEL-015
    public void testTheEditorsRememberedOpenAgainAndOneNoLongerReadIsLeftOut() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode checkout = aTestSetIn(testProject, "Checkout");
        final @NotNull TestSetNode payments = aTestSetIn(testProject, "Payments");
        PropertiesComponent.getInstance(getProject()).setValue(OPEN_EDITORS, checkout.getPath() + ";" + root.resolve("Gone") + ";" + payments.getPath());
        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);

        Services.getInstance(getProject(), LastOpenEditors.class).reopen();

        Await.until("the remembered editors were not opened again", () -> editors.openNodePaths().size() == 2);
        assertEquals("the editors opened again are not the two still read", Set.of(checkout.getPath(), payments.getPath()), Set.copyOf(editors.openNodePaths()));
        assertEquals("the remembered editors were kept after they opened", "", Objects.toString(PropertiesComponent.getInstance(getProject()).getValue(OPEN_EDITORS), ""));
    }

    // UC-INTERNAL-002
    public void testNothingRememberedOpensNothing() {
        Services.getInstance(getProject(), LastOpenEditors.class).reopen();

        assertTrue("an editor opened though none was remembered", Services.getInstance(getProject(), TestinEditors.class).openNodePaths().isEmpty());
    }
}
