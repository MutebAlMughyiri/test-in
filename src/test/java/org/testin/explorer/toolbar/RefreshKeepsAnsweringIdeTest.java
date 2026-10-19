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

import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.model.TestCase;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.UUID;

public class RefreshKeepsAnsweringIdeTest extends AbstractReadTheRootIdeTest {

    // UC-TREE-PANEL-025, Rule-INTERNAL-081
    public void testWhileRefreshReadsAgainTheIndexAnswersAndASaveKeepsWhenTheTestCaseWasCreated() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path login = marked(theTestCasesOf(checkout).resolve("Login"), NodeType.TEST_SET);
        final @NotNull UUID testCaseUuid = aTestCaseIn(login);
        final @NotNull Path cycle = marked(theTestRunsOf(checkout).resolve("Cycle-1"), NodeType.TEST_RUN);
        runItemIn(cycle, FileKind.RUN_ITEM.fileName(testCaseUuid));
        readEverything();
        final @NotNull ZonedDateTime createdAt = indexedTestCases().findTestCase(testCaseUuid).orElseThrow().getCreatedAt();

        indexer().readAgain();

        assertTrue("Refresh emptied the test cases out of the index before reading again", indexedTestCases().findTestCase(testCaseUuid).isPresent());
        assertTrue("Refresh emptied the test runs out of the index before reading again, so a run item status recorded then is refused", nodes().nodeExists(cycle));

        final @NotNull TestCase held = indexedTestCases().findTestCase(testCaseUuid).orElseThrow();
        assertTrue("the save was refused", indexedTestCases().putTestCase(login, held.edit().description("Log in with a locked user").build()));
        assertEquals("a test case saved while Refresh read again was stamped as created now", createdAt, indexedTestCases().findTestCase(testCaseUuid).orElseThrow().getCreatedAt());

        indexer().indexWithProgress();
        Await.until("Refresh never finished reading again", () -> indexer().isIndexed());
        assertTrue("the test case is not there once Refresh has read again", indexedTestCases().findTestCase(testCaseUuid).isPresent());
    }
}
