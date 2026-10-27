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

package org.testin.explorer.toolbar;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.TreeGesture;
import org.testin.config.TestinYml;
import org.testin.explorer.TreePanel;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class RefreshRereadsTheBindingIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull Optional<String> ymlWas = Optional.empty();

    @Override
    protected void setUp() {
        super.setUp();
        try {
            ymlWas = Files.exists(yml()) ? Optional.of(Files.readString(yml())) : Optional.empty();
            Files.createDirectories(yml().getParent());
        } catch (final IOException ex) {
            throw new AssertionError("could not keep " + TestinYml.fileName() + " aside: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void tearDown() {
        try {
            if (ymlWas.isPresent()) Files.writeString(yml(), ymlWas.orElseThrow());
            else Files.deleteIfExists(yml());
        } catch (final IOException ex) {
            throw new AssertionError("could not put " + TestinYml.fileName() + " back: " + ex.getMessage(), ex);
        }
        TestinYml.reload(getProject());
        super.tearDown();
    }

    private @NotNull Path yml() {
        return TestinYml.savePath(getProject()).orElseThrow(() -> new AssertionError("the project has no " + TestinYml.fileName()));
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private void editedByHand(final @NotNull String testProject) {
        try {
            Files.writeString(yml(), "testinProject: " + testProject + System.lineSeparator());
        } catch (final IOException ex) {
            throw new AssertionError("could not edit " + TestinYml.fileName() + ": " + ex.getMessage(), ex);
        }
    }

    // Rule-TREE-PANEL-081
    public void testRefreshPicksUpATestProjectChangedByHand() {
        aTestProjectAt(root.resolve("Checkout"));
        aTestProjectAt(root.resolve("Payments"));
        editedByHand("Checkout");
        TestinYml.reload(getProject());
        readEverything();
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
        Await.until("the panel never showed the test project " + TestinYml.fileName() + " names", panel::showsTree);
        assertEquals("Checkout", bound().name());

        editedByHand("Payments");
        assertEquals("the hand edit was read before Refresh", "Checkout", bound().name());

        TreeGesture.pressed(getProject(), panel.getRefreshAction(), List.of());

        Await.until("Refresh did not pick up the test project changed by hand", () -> bound().name().equals("Payments") && bound().get().isPresent());
    }
}
