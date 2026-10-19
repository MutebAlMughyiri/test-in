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

package org.testin.setting;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.services.Services;
import org.testin.view.Drawn;

import java.util.List;

public class AgentTimeoutIdeTest extends AbstractIdeTest {

    private int wasStored;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        wasStored = settings().agentTimeoutSeconds;
    }

    @Override
    protected void tearDown() {
        settings().agentTimeoutSeconds = wasStored;
        super.tearDown();
    }

    private static @NotNull AgentPromptConfigurable typed(final @NotNull String timeout) {
        final @NotNull AgentPromptConfigurable page = new AgentPromptConfigurable();
        Drawn.first(page.createComponent(), JBTextField.class).setText(timeout);
        return page;
    }

    // Rule-CODEGEN-115
    public void testATimeoutThatIsNotSecondsAboveZeroIsRefusedAndTheSavedOneIsKept() {
        settings().agentTimeoutSeconds = 240;

        for (final String mistyped : List.of("6O", "0", "-5")) {
            try {
                typed(mistyped).apply();
                fail("the timeout " + mistyped + " was saved");
            } catch (final ConfigurationException refused) {
                assertTrue("the refusal does not say what was typed: " + refused.getMessageHtml(), refused.getMessageHtml().toString().contains(mistyped));
            }

            assertEquals("a refused timeout changed the one saved before", 240, settings().agentTimeoutSeconds);
        }
    }

    // Rule-CODEGEN-115
    public void testAWholeNumberOfSecondsIsSaved() {
        try {
            typed("90").apply();
        } catch (final ConfigurationException ex) {
            throw new AssertionError("a timeout of 90 seconds was refused", ex);
        }

        assertEquals("the timeout typed was not saved", 90, settings().agentTimeoutSeconds);
    }
}
