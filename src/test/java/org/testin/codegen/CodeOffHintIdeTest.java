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


package org.testin.codegen;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Said;
import org.testin.config.TestinYml;
import org.testin.help.Hints;
import org.testin.help.SetupHint;
import org.testin.help.SetupStep;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CodeOffHintIdeTest extends AbstractIdeTest {

    private @NotNull List<SetupHint> waiting() {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == SetupStep.TEST_PROJECT_LINK).toList();
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public void testCodeOffIsAHintWithSaveToTestinYmlAndNoMessage() {
        final @NotNull List<String> said = Said.during(getProject(), () -> assertFalse("code is on with no testin.yml", CodeOn.isOnOrHinted(getProject(), CodeChangeRegistry.need(getProject()))));

        assertEquals("a message was raised while code updated", List.of(), said);
        assertEquals("code off did not wait as one hint", 1, waiting().size());
        assertTrue("the hint carries no Save to testin.yml", waiting().getFirst().form().isPresent());
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public void testAPressedKeyStillAnswersAndTheHintWaitsBesideIt() {
        final @NotNull List<String> said = Said.during(getProject(), () -> assertTrue(CodeOn.isOffAndWarned(getProject(), CodeChangeRegistry.need(getProject()))));

        assertEquals("the key was not answered", 1, said.size());
        assertEquals("code off did not wait as one hint", 1, waiting().size());
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public void testChoosingTheTestProjectTestinYmlNamesClearsTheHint() {
        final @NotNull Path folder = TestinYml.savePath(getProject()).map(Path::getParent).orElseThrow(() -> new AssertionError("the project has no folder for " + TestinYml.fileName()));
        try {
            Files.createDirectories(folder);
        } catch (final IOException ex) {
            throw new AssertionError("could not make the project's folder " + folder + ": " + ex.getMessage(), ex);
        }
        assertTrue("could not write " + TestinYml.fileName(), TestinYml.save(getProject(), TestinYml.lines("NAFATH")));
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);
        try {
            bound.choose("Shop");
            assertFalse("code is on for a test project testin.yml does not name", CodeOn.isOnOrHinted(getProject(), CodeChangeRegistry.need(getProject())));
            assertEquals(1, waiting().size());

            bound.choose("NAFATH");

            assertEquals("choosing the test project testin.yml names left the hint waiting", List.of(), waiting());
        } finally {
            TestinYml.save(getProject(), TestinYml.lines(""));
            bound.choose("");
            Services.getInstance(getProject(), Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
        }
    }
}
