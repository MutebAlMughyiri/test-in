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

package org.testin.model;


import org.jetbrains.annotations.NotNull;
import org.testin.model.status.TestCaseStatus;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class ByteIdenticalSaveTest {

    private static final Path GOLDEN = Path.of("src", "test", "resources", "golden", "test-case.json");

    private static @NotNull TestCaseDto typedByATester() {
        final TestCaseDto testCase = new TestCaseDto();

        testCase.setId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        testCase.setOrder("zo");
        testCase.setDescription("Log in with a valid user");
        testCase.setExpectedResult("The tester reaches the dashboard.");
        testCase.setSteps(List.of("Open \"/login\"", "Type C:\\Users\\test", "Press Enter"));
        testCase.setStatus(TestCaseStatus.TO_BE_UPDATED);
        testCase.setPriority(Priority.HIGH);
        testCase.setReference("JIRA-1");
        testCase.setGroups(List.of("smoke", "régression"));
        testCase.setCreatedBy("Muteb");
        testCase.setUpdatedBy("");
        testCase.setCreatedAt(ZonedDateTime.of(2026, 9, 2, 23, 29, 28, 0, ZoneId.of("Asia/Riyadh")));
        testCase.setUpdatedAt(ZonedDateTime.of(2026, 9, 10, 1, 23, 59, 0, ZoneId.of("Europe/London")));
        testCase.setModule("");
        testCase.setTestData("{\"user\": \"admin\"}");
        testCase.setPreConditions("");

        return testCase;
    }

    private static @NotNull String saved() {
        return new String(RealJsonCodec.build().writeValueAsBytes(typedByATester()), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static @NotNull String golden() {
        try {
            if (!Files.exists(GOLDEN)) {
                Files.createDirectories(GOLDEN.getParent());
                Files.writeString(GOLDEN, saved(), StandardCharsets.UTF_8);

                throw new AssertionError("Wrote a new fixture at " + GOLDEN.toAbsolutePath()
                        + " - read it, check it is what a tester's file should look like, and run again.");
            }

            return Files.readString(GOLDEN, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (final IOException ex) {
            throw new AssertionError("Could not read or write the golden file: " + GOLDEN.toAbsolutePath(), ex);
        }
    }

    // Rule-PRODUCT-003, Rule-EDITOR-PANEL-005
    @Test
    public void aSavedTestCaseIsByteIdenticalToTheFixture() {
        assertEquals(saved(), golden(),
                "The bytes a test case saves as have changed. If that was deliberate, update "
                        + GOLDEN + " and say in the commit what a tester now sees differently - "
                        + "and if it was not, something reformatted a value on the way to disk.");
    }

    // Rule-INTERNAL-070
    @Test
    public void theMachineTimeZoneDoesNotReachTheFile() {
        final TimeZone was = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"));
            final String riyadh = saved();

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
            final String auckland = saved();

            assertEquals(auckland, riyadh,
                    "The same test case saved differently in two time zones, so test data shared through Git"
                            + " churns whenever a colleague elsewhere opens and saves it.");
        } finally {
            TimeZone.setDefault(was);
        }
    }

    @Test
    public void nothingTypedIsNothingStored() {
        final String json = saved();

        assertFalse(json.contains("\"module\""), "an empty module was written: " + json);
        assertFalse(json.contains("\"preConditions\""), "empty preconditions were written: " + json);
        assertFalse(json.contains("\"updatedBy\""), "an empty updatedBy was written: " + json);
    }

    // Rule-INTERNAL-135
    @Test
    public void aTestCaseAtItsDefaultPriorityAndStatusStoresNeither() {
        final String json = RealJsonCodec.build().writeValueAsString(TestCaseDto.builder().description("Log in with a valid user").build());

        assertFalse(json.contains("\"priority\""), "the default priority, Low, was written: " + json);
        assertFalse(json.contains("\"status\""), "the default status, Pending, was written: " + json);
        assertFalse(json.contains("\"groups\""), "an empty list of groups was written: " + json);
    }
}
