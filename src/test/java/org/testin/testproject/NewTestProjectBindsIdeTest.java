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

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.explorer.TreePanel;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.services.Services;

public class NewTestProjectBindsIdeTest extends AbstractReadTheRootIdeTest {

    // Rule-TREE-PANEL-017
    public void testCreatingATestProjectBindsThisCodeProjectToIt() {
        aTestProjectAt(root.resolve("Checkout"));
        aTestProjectAt(root.resolve("Payments"));
        readEverything();
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);
        assertEquals("nothing should be bound among two test projects", "", bound.name());

        new NewTestProject(getProject(), panel, "Mobile").execute();

        assertEquals("creating a test project did not bind this code project to it", "Mobile", bound.name());
        Await.until("the panel did not open the test project it created", () -> panel.showsTree() && bound.get().isPresent());
    }
}
