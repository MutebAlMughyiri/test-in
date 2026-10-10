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

import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class BugFactsTest {

    // Rule-VIEW-PANEL-068
    @Test
    public void theFactsAreReadOffTheRunItemItsTestCaseAndItsTestRun() {
        final UUID id = UUID.randomUUID();
        final TestCaseDto tc = TestCaseDto.builder().id(id).description("  Log in with a valid user ").expectedResult("Welcome")
                .steps(List.of("Open", "Log in")).testData("user=a").build();
        final RunItem runItem = RunItem.builder().id(id).status(RunItemStatus.FAILED).actualResult("Error page")
                .bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).stacktrace("boom")
                .executedBy("Muteb").executedAt(ZonedDateTime.of(2026, 9, 13, 14, 14, 0, 0, ZoneId.of("Asia/Riyadh"))).build();
        final TestRunMarker testRunMarker = new TestRunMarker();
        testRunMarker.setConfiguration(new EnumMap<>(Map.of(
                TestRunConfiguration.PLATFORM, "Web",
                TestRunConfiguration.BROWSER, "Chrome",
                TestRunConfiguration.COMMIT, "933a3984")));

        final BugFacts facts = BugFacts.of(runItem, tc, testRunMarker, "Sprint 7", List.of());

        assertEquals(facts.title(), "Log in with a valid user.", "the description as the Details tab shows it");
        assertEquals(facts.expectedResult(), "Welcome.");
        assertEquals(facts.steps(), List.of("Open.", "Log in."), "each step as the Steps row formats it");
        assertEquals(facts.actualResult(), "Error page", "shown as typed in the Details tab, so copied as typed");
        assertEquals(facts.testData(), "user=a");
        assertEquals(facts.platform(), "Web", "an unanswered component is left out, not joined");
        assertEquals(facts.executed(), "Muteb · Sunday 13-09-2026 At 14:14:00 [Asia/Riyadh]");
        assertEquals(facts.browser(), "Chrome");
        assertEquals(facts.device(), "");
        assertEquals(facts.commit(), "933a3984");
        assertEquals(facts.stacktrace(), "boom");
        assertEquals(facts.testRun(), "Sprint 7");
        assertEquals(facts.testCaseId(), id);
    }

    @Test
    public void aRunItemNobodyExecutedHasNoExecuted() {
        final TestCaseDto tc = TestCaseDto.builder().build();
        final RunItem runItem = RunItem.builder().id(tc.getId()).build();

        assertEquals(BugFacts.of(runItem, tc, new TestRunMarker(), "Sprint 7", List.of()).executed(), "");
    }
}
