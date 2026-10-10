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

package org.testin.rename;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.model.node.TestSetNode;
import org.testin.util.NameSanitizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class RenamePutBackIdeTest extends AbstractCodegenIdeTest {

    private static void deleteFolder(final @NotNull Path folder) {
        try (Stream<Path> tree = Files.walk(folder)) {
            for (final Path each : tree.sorted(Comparator.reverseOrder()).toList()) Files.delete(each);
        } catch (final IOException ex) {
            throw new AssertionError("Could not delete " + folder + ": " + ex.getMessage(), ex);
        }
    }

    // Rule-TREE-PANEL-133
    public void testARenameWhoseFolderCannotBeRenamedPutsTheCodeBack() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull String renamedClass = "nafath." + NameSanitizer.className("Sign in");
        deleteFolder(login.getPath());

        final @NotNull AtomicBoolean done = new AtomicBoolean();
        NodeRename.apply(getProject(), login, "Sign in", () -> done.set(true));
        assertTrue("the code was not renamed before the folder", generatedClass(renamedClass).isPresent());

        Await.until("the code kept the new name after its folder could not be renamed", () -> generatedClass(renamedClass).isEmpty() && generatedClass("nafath.LoginTest").isPresent());
        assertFalse("a rename whose folder could not be renamed was reported as done", done.get());
    }
}
