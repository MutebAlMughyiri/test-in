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


package org.testin.help;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.ActionLink;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HelpIdeTest extends BasePlatformTestCase {

    private @NotNull Hints nothingWaits() {
        final @NotNull Hints hints = Services.getInstance(getProject(), Hints.class);
        Arrays.stream(SetupStep.values()).forEach(hints::clear);
        Services.getInstance(getProject(), Guides.class).forgetAll();
        return hints;
    }

    // Rule-INTERNAL-127
    public void testAFiredHintWaitsUntilItsStepIsCleared() {
        final @NotNull Hints hints = nothingWaits();

        hints.fire(Hint.of(SetupStep.BUG_FILING, "Add bugRepoUrl to testin.yml"));
        hints.fire(Hint.of(SetupStep.BUG_FILING, "Not signed in to github.com"));
        assertEquals("a second hint on one step did not replace the first", List.of("Not signed in to github.com"), hints.waiting().stream().map(Hint::text).toList());

        hints.clear(SetupStep.BUG_FILING);
        assertTrue("a cleared hint still waits", hints.waiting().isEmpty());
    }

    // Rule-INTERNAL-127
    public void testStepsThatShareAGuideWaitAndClearApart() {
        final @NotNull Hints hints = nothingWaits();

        hints.fire(Hint.of(SetupStep.BUG_FILING, "Add bugRepoUrl to testin.yml"));
        hints.fire(Hint.of(SetupStep.BOARD_COLUMNS, "Run gh auth refresh -s read:project"));
        hints.clear(SetupStep.BUG_FILING);

        assertEquals("a ready Send cleared the board hint too", List.of(SetupStep.BOARD_COLUMNS), hints.waiting().stream().map(Hint::step).toList());
        hints.clear(SetupStep.BOARD_COLUMNS);
    }

    // Rule-INTERNAL-128, Rule-INTERNAL-129
    public void testAFiredHintOffersItsGuide() {
        final @NotNull Hints hints = nothingWaits();

        hints.fire(Hint.of(SetupStep.TESTIN_FOLDER, "Please set the Testin folder"));

        assertEquals(List.of(Guide.SET_UP_THIS_MACHINE), Services.getInstance(getProject(), Guides.class).offered());
        hints.clear(SetupStep.TESTIN_FOLDER);
    }

    // Rule-INTERNAL-127
    public void testALinkFormRunsItsFix() {
        final @NotNull AtomicInteger fixed = new AtomicInteger();
        final @NotNull Hint hint = Hint.of(SetupStep.TEST_PROJECT_LINK, "testin.yml does not name this test project", "Save to testin.yml", fixed::incrementAndGet);

        final @NotNull ActionLink link = (ActionLink) hint.form().orElseThrow().get();
        link.doClick();

        assertEquals("Save to testin.yml", link.getText());
        assertEquals("the link did not run its fix", 1, fixed.get());
    }

    // Rule-INTERNAL-129
    public void testOfferedGuidesListInGuideOrderForTheSession() {
        nothingWaits();
        final @NotNull Guides guides = Services.getInstance(getProject(), Guides.class);

        guides.add(Guide.RAISE_BUG_REPORTS);
        guides.add(Guide.SET_UP_THIS_MACHINE);
        guides.add(Guide.RAISE_BUG_REPORTS);

        assertEquals("the offered guides are not in guide order, once each", List.of(Guide.SET_UP_THIS_MACHINE, Guide.RAISE_BUG_REPORTS), guides.offered());
    }
}
