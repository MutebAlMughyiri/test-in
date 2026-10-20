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
package org.testin;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class NamesSayTheBusinessWordTest {

    private static final @NotNull List<Path> ROOTS = ContentModules.each("src");

    private static final @NotNull Pattern BARE = Pattern.compile("(?<!Test)Run(?!Item|ner|ning|nable)|(?<!Test)(?<!Use)Cases?(?![a-z])|Verdict"
            + "|(?<!Run)(?<!Toolbar)(?<!StatusBar)(?<!Menu)Items?(?![a-z])"
            + "|(?<!Test)(?<!Testin)Projects?(?![a-z])"
            + "|(?<!TestCase)(?<!RunItem)(?<!TestRun)(?<!AutomationExecution)(?<!Node)(?<!TestSet)(?<!Package)(?<!TestProject)(?<!Git)Status(?!Bar)");

    private static final @NotNull Set<String> CODE_PROJECT = Set.of("AbstractAnyProjectAction", "ProjectLifetime", "SaveOnProjectClose", "SaveOnProjectCloseIdeTest");

    private static @NotNull List<String> bareTypeNames() {
        final @NotNull List<String> found = new ArrayList<>();

        for (final Path root : ROOTS) {
            assertTrue(Files.isDirectory(root), root + " is missing - this test expects to run from the project root");

            try (Stream<Path> files = Files.walk(root)) {
                files.map(file -> file.getFileName().toString())
                        .filter(name -> name.endsWith(".java"))
                        .map(name -> name.substring(0, name.length() - ".java".length()))
                        .filter(name -> BARE.matcher(name).find() && !CODE_PROJECT.contains(name))
                        .forEach(found::add);
            } catch (final IOException ex) {
                throw new AssertionError("could not walk " + root, ex);
            }
        }

        return found;
    }

    @Test
    public void noTypeIsNamedWithABareRunCaseItemProjectStatusOrVerdict() {
        assertEquals(bareTypeNames(), List.of(),
                "A type name says the business word in full: Test Run or Run Item, never Run alone; Test Case, never Case;"
                        + " Run Item or Test Case, never Item; Test Project, never Project, unless it is IntelliJ's project;"
                        + " which status it is, never Status alone; Run Item Status, never Verdict; Automation Execution Status, never Execution Status.");
    }

    @Test
    public void theScanTellsTheFullWordFromTheBareOne() {
        assertTrue(BARE.matcher("RunEditor").find());
        assertTrue(BARE.matcher("NextCaseAction").find());
        assertTrue(BARE.matcher("VerdictDonut").find());
        assertFalse(BARE.matcher("TestRunEditor").find());
        assertFalse(BARE.matcher("RunItemStatus").find());
        assertFalse(BARE.matcher("TestCaseCard").find());
        assertFalse(BARE.matcher("TestNGRunner").find(), "a runner is a tool, not a test run");
        assertTrue(BARE.matcher("ItemRecorder").find());
        assertTrue(BARE.matcher("ProjectIndexer").find());
        assertTrue(BARE.matcher("UpdateStatusAction").find());
        assertFalse(BARE.matcher("ToolbarItem").find());
        assertFalse(BARE.matcher("TestProjectIndexer").find());
        assertFalse(BARE.matcher("TestinProjectConfig").find());
        assertFalse(BARE.matcher("UpdateNodeStatusAction").find());
        assertTrue(BARE.matcher("ExecutionStatus").find(), "an execution of a run item has a status too, so TestNG's says it is TestNG's");
        assertFalse(BARE.matcher("AutomationExecutionStatus").find());
        assertFalse(BARE.matcher("EditorStatusBar").find(), "the status bar is a place on screen, not a status");
    }
}
