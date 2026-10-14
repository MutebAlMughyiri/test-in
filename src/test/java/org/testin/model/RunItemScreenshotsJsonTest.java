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

import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RunItemScreenshotsJsonTest {

    private static final String NAME = "k3f9a.png";

    @Test
    public void theTestRunFileNamesTheScreenshotsAndHoldsNoPicture() {
        final RunItem written = RunItem.builder().uuid(UUID.randomUUID()).executions(List.of(RunItemExecution.builder().stacktrace("boom").screenshots(List.of(NAME)).build())).build();

        try {
            final String json = new String(RealJsonCodec.build().writeValueAsBytes(written), StandardCharsets.UTF_8);
            final RunItem read = RealJsonCodec.build().readValue(json, RunItem.class);

            assertTrue(json.contains("\"" + NAME + "\""), "the file is named: " + json);
            assertFalse(json.contains("iVBORw"), "and no base64 of the picture is written: " + json);
            assertEquals(read.shownExecution().getScreenshots(), List.of(NAME));
            assertEquals(read.shownExecution().getStacktrace(), "boom", "the text stays text");
        } catch (final Exception ex) {
            throw new AssertionError("the run item could not be written and read back", ex);
        }
    }

    @Test
    public void aRunItemWithNoScreenshotWritesNoKey() {
        try {
            final String json = new String(RealJsonCodec.build().writeValueAsBytes(RunItem.builder().uuid(UUID.randomUUID()).build()), StandardCharsets.UTF_8);

            assertFalse(json.contains("screenshots"), json);
        } catch (final Exception ex) {
            throw new AssertionError("the run item could not be written", ex);
        }
    }

    @Test
    public void aNewScreenshotNameIsShortAndNotOneTheTestRunHolds() {
        final Set<String> taken = new HashSet<>();

        IntStream.range(0, 1000).forEach(_ -> {
            final String name = TestRunNode.newScreenshotName(taken);

            assertTrue(name.matches("[0-9a-z]{5}\\.png"), name);
            assertTrue(taken.add(name), "a name the test run already holds: " + name);
            assertTrue(TestRunNode.isScreenshotName(name));
        });

        assertFalse(TestRunNode.isScreenshotName("Cycle 1.testrun"));
        assertFalse(TestRunNode.isScreenshotName("my notes.png"), "a PNG put there by hand under another name is not a screenshot");
    }
}
