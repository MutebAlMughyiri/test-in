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

import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class BugLimitsTest {

    @Test
    public void aTitleIsNeededAndHasALimit() {
        assertEquals(BugLimits.whyNot("", "body", 0), Optional.of(Bundle.message("bug.limit.title.empty")));
        assertEquals(BugLimits.whyNot("   ", "body", 0), Optional.of(Bundle.message("bug.limit.title.empty")));
        assertEquals(BugLimits.whyNot("a".repeat(256), "body", 0), Optional.empty());
        assertEquals(BugLimits.whyNot("a".repeat(257), "body", 0), Optional.of(Bundle.message("bug.limit.title.long", 257, 256)));
        assertEquals(BugLimits.whyNot("  " + "a".repeat(256) + "  ", "body", 0), Optional.empty(), "the spaces around it are not sent");
    }

    @Test
    public void theBodyHasALimit() {
        assertEquals(BugLimits.whyNot("title", "b".repeat(65_536), 0), Optional.empty());
        assertEquals(BugLimits.whyNot("title", "b".repeat(65_537), 0), Optional.of(Bundle.message("bug.limit.body.long", 65_537, 65_536)));
    }

    @Test
    public void ghAttachesFiftyScreenshotsAtMost() {
        assertEquals(BugLimits.whyNot("title", "body", 50), Optional.empty());
        assertEquals(BugLimits.whyNot("title", "body", 51), Optional.of(Bundle.message("bug.limit.screenshots", 51, 50)));
    }
}
