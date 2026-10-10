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

package org.testin.report;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class ReportFileNameTest {

    private static final @NotNull ZonedDateTime AT =
            ZonedDateTime.of(2026, 8, 25, 21, 40, 15, 0, ZoneId.of("Asia/Riyadh"));

    // Rule-REPORT-006
    @Test
    public void itSaysTheProjectTheTestRunAndWhen() {
        assertEquals(ReportFileName.of("Nafath", "Sprint 7 Cycle 3", AT),
                "TestRun_Nafath_Sprint7Cycle3_25-08-2026_09-40-15PM");
    }

    @Test
    public void theTimeCarriesNoColon() {
        final @NotNull String name = ReportFileName.of("Nafath", "Sprint 7", AT);

        assertFalse(name.contains(":"), "a colon in a file name is refused by Windows outright: " + name);
        assertTrue(name.endsWith("PM"), "the hour is the tester's own twelve-hour clock: " + name);
    }

    // Rule-REPORT-007
    @Test
    public void aTestRunNamedWithPunctuationStillMakesAFileName() {
        final @NotNull String name = ReportFileName.of("Nafath", "API / UI: v1.2", AT);

        for (final String forbidden : new String[]{"/", "\\", ":", "*", "?", "\"", "<", ">", "|"}) {
            assertFalse(name.contains(forbidden), "kept " + forbidden + " in " + name);
        }
    }

    // Rule-REPORT-008
    @Test
    public void anUnnamedProjectLeavesNoGap() {
        assertEquals(ReportFileName.of("", "Sprint 7", AT), "TestRun_Sprint7_25-08-2026_09-40-15PM");
    }

    // Rule-REPORT-007
    @Test
    public void theNameCarriesNoSpaces() {
        assertFalse(ReportFileName.of("Nafath Test", "Sprint 7 Cycle 3", AT).contains(" "));
    }

    @Test
    public void aSecondReportOfTheSameTestRunIsASecondFile() {
        assertNotEquals(ReportFileName.of("Nafath", "Sprint 7", AT.plusSeconds(1)), ReportFileName.of("Nafath", "Sprint 7", AT));
    }
}
