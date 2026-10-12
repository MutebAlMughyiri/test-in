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


package org.testin.services;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.util.Bundle;

import java.util.List;

public class PluginHintIdeTest extends BasePlatformTestCase {

    private @NotNull List<String> hintsFor(final @NotNull SetupStep step) {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == step).map(SetupHint::text).toList();
    }

    // Rule-CODEGEN-005, Rule-INTERNAL-127
    public void testAMissingPluginFoundOnItsOwnIsAHintAndNothingElse() {
        OptionalPlugin.TESTNG.missingUntil(getTestRootDisposable());

        final @NotNull List<String> said = Said.during(getProject(), () -> assertFalse(OptionalPlugin.TESTNG.isAvailableOrHinted(getProject())));

        assertEquals("a message was raised for a plugin nobody asked for", List.of(), said);
        assertEquals(List.of(Bundle.message("plugin.testng.requirement")), hintsFor(SetupStep.TESTNG_PLUGIN));
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.TESTNG_PLUGIN);
    }

    // Rule-CODEGEN-062, Rule-INTERNAL-127
    public void testAPressedKeyNeedingAMissingPluginStillAnswersAndTheHintWaits() {
        OptionalPlugin.GIT.missingUntil(getTestRootDisposable());

        final @NotNull List<String> said = Said.during(getProject(), () -> assertTrue(OptionalPlugin.GIT.isMissingAndWarned(getProject())));

        assertEquals("the key was not answered", 1, said.size());
        assertEquals(List.of(Bundle.message("plugin.git.requirement")), hintsFor(SetupStep.GIT_PLUGIN));
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.GIT_PLUGIN);
    }
}
