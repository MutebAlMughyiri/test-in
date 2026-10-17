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

package org.testin.testrun.form;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.AbstractButton;
import java.util.EnumMap;
import java.util.Map;

public class TestRunConfigurationFormIdeTest extends AbstractIdeTest {

    private static void choose(final @NotNull TestRunConfigurationForm form, final @NotNull String answer) {
        ((AbstractButton) Drawn.reading(form.getPanel(), answer)).doClick();
    }

    private static @NotNull AbstractButton frontend(final @NotNull TestRunConfigurationForm form) {
        return (AbstractButton) Drawn.reading(form.getPanel(), "Frontend");
    }

    private static @NotNull String savedBrowser(final @NotNull TestRunConfigurationForm form) {
        return form.configuration().getOrDefault(TestRunConfiguration.BROWSER, "");
    }

    // Rule-TREE-PANEL-120
    public void testABrowserIsAskedAndSavedOnlyWhileTheAnswersAboveCallForIt() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        answers.put(TestRunConfiguration.PLATFORM, "Web");
        answers.put(TestRunConfiguration.COMPONENT, "Backend");
        answers.put(TestRunConfiguration.BROWSER, "Chrome");

        final @NotNull TestRunConfigurationForm form = new TestRunConfigurationForm(getProject(), "Cycle-1", answers);
        assertEquals("a backend test run saved the browser row it was never asked", "", savedBrowser(form));

        choose(form, "Frontend");
        assertEquals("the browser row did not come back when the answer above it changed", "Chrome", savedBrowser(form));

        choose(form, "Backend");
        assertEquals("the browser row stayed when the answer above it changed back", "", savedBrowser(form));
    }

    // Rule-TREE-PANEL-144
    public void testAnIntegrationTestRunIsOnTheBackendAndFrontendCannotBePicked() {
        final @NotNull TestRunConfigurationForm form = new TestRunConfigurationForm(getProject(), "Cycle-1", new EnumMap<>(TestRunConfiguration.class));
        choose(form, "Frontend");

        choose(form, "Integration");

        assertEquals("Integration did not make the component Backend", "Backend", form.configuration().get(TestRunConfiguration.COMPONENT));
        assertFalse("Frontend can be picked for an Integration test run", frontend(form).isEnabled());
        assertEquals("the gray Frontend does not say why", Bundle.message("config.component.backend.only"), Drawn.hovering(frontend(form)));

        choose(form, "Web");

        assertTrue("Frontend stayed gray once the platform was Web", frontend(form).isEnabled());
        assertEquals("the component did not stay Backend", "Backend", form.configuration().get(TestRunConfiguration.COMPONENT));
    }
}
