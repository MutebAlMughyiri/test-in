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

package org.testin.bug;

import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.Execution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BugIssueStatesTest {

    private static @NotNull RunItems filed(final @NotNull String... bugIssueUrls) {
        final @NotNull List<RunItem> runItems = new ArrayList<>();
        for (final String url : bugIssueUrls) {
            runItems.add(RunItem.builder().id(UUID.randomUUID()).bugIssueUrl(url).executions(List.of(Execution.builder().status(RunItemStatus.FAILED).build())).build());
        }
        return RunItems.builder().all(runItems).build();
    }

    // Rule-VIEW-PANEL-092
    @Test
    public void aTestProjectWithNoFiledBugHasNothingToAsk() {
        assertTrue(BugIssueStates.filedIn(List.of(filed(), RunItems.builder().build())).isEmpty());
    }

    // Rule-VIEW-PANEL-092
    @Test
    public void everyBugInOneRepositoryIsOneRequestWhicheverTestRunFiledIt() {
        final @NotNull Map<BugRepository, Set<Integer>> filed = BugIssueStates.filedIn(List.of(
                filed("https://github.com/mtb550/test-03/issues/14", "https://github.com/mtb550/test-03/pull/2"),
                filed("https://github.com/mtb550/test-03/issues/9", "https://ghe.acme.com/qa/product/issues/14")));

        assertEquals(filed, Map.of(
                new BugRepository("github.com", "mtb550", "test-03"), Set.of(9, 14),
                new BugRepository("ghe.acme.com", "qa", "product"), Set.of(14)));
    }
}
