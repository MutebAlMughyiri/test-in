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

package org.testin.view.details;

import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.indexer.TestCases;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class CommittedTestRunF2IdeTest extends AbstractReadTheRootIdeTest {

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-110
    public void testF2OnARunItemOfACommittedTestRunChangesNothingAndSaysWhere() {
        final @NotNull Path testProject = aTestProjectAt(root.resolve("Shop"));
        final @NotNull Path testSet = marked(theTestCasesOf(testProject).resolve("Login"), NodeType.TS);
        final @NotNull UUID id = aTestCaseIn(testSet);
        final @NotNull Path testRun = theTestRunsOf(testProject).resolve("Cycle 4");
        try {
            Files.createDirectories(testRun);
            Files.writeString(testRun.resolve(NodeType.TR.getMarker()), """
                    {
                      "createdBy" : "Sara",
                      "createdAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
                      "status" : "COMMITTED",
                      "commit" : "ea9a50107afbbaa1831909436b781f0e3c2d1a55"
                    }""", StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write the test run: " + ex.getMessage(), ex);
        }
        resultIn(testRun, FileKind.RUN_ITEM.fileName(id), id);
        readEverything();
        final @NotNull TestCaseDto shown = Services.getInstance(getProject(), TestCases.class).findTestCase(id).orElseThrow();

        final @NotNull List<String> said = Said.during(getProject(), () -> EditShownTestCase.openIfEditable(getProject(), shown, List.of("Shop", "Test Runs", "Cycle 4")));

        assertTrue("F2 did not say the test run keeps the test case: " + said, said.stream().anyMatch(line -> line.contains(Bundle.message("details.committed.no.edit"))));
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-110
    public void testF2OnADeletedTestCaseChangesNothingAndSaysWhy() {
        final @NotNull Path testProject = aTestProjectAt(root.resolve("Shop"));
        final @NotNull Path login = marked(theTestCasesOf(testProject).resolve("Login"), NodeType.TS);
        final @NotNull UUID id = aTestCaseIn(login);
        try {
            Files.delete(login.resolve(FileKind.TEST_CASE.fileName(id)));
        } catch (final IOException ex) {
            throw new AssertionError("Could not delete the test case: " + ex.getMessage(), ex);
        }
        readEverything();

        final @NotNull List<String> said = Said.during(getProject(), () -> EditShownTestCase.openIfEditable(getProject(), TestCaseDto.deleted(id), List.of("Shop", "Test Cases", "Login")));

        assertTrue("F2 did not say the test case was deleted: " + said, said.stream().anyMatch(line -> line.contains(Bundle.message("details.deleted.no.edit"))));
    }
}
