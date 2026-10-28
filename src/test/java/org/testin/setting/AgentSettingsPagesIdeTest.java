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

import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.util.List;

public class AgentSettingsPagesIdeTest extends AbstractIdeTest {

    private @NotNull String commandWas = "";

    private @NotNull String argumentsWas = "";

    private @NotNull String promptWas = "";

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull List<JBTextField> fieldsOn(final @NotNull JComponent shown) {
        return Drawn.components(shown).stream().filter(JBTextField.class::isInstance).map(JBTextField.class::cast).toList();
    }

    private static @NotNull JBTextArea promptOn(final @NotNull JComponent shown) {
        return Drawn.components(shown).stream().filter(JBTextArea.class::isInstance).map(JBTextArea.class::cast).findFirst().orElseThrow(() -> new AssertionError("the page has no prompt"));
    }

    private static void pressed(final @NotNull JComponent shown, final @NotNull String button) {
        Drawn.components(shown).stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(candidate -> button.equals(candidate.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the page has no " + button + " button"))
                .doClick();
    }

    @Override
    protected void setUp() {
        super.setUp();
        commandWas = settings().agentCommand;
        argumentsWas = settings().agentArguments;
        promptWas = settings().agentPrompt;
        settings().agentCommand = "claude";
        settings().agentArguments = "-p";
        settings().agentPrompt = "";
    }

    @Override
    protected void tearDown() {
        settings().agentCommand = commandWas;
        settings().agentArguments = argumentsWas;
        settings().agentPrompt = promptWas;
        super.tearDown();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    public void testTheConnectionIsTheCommandAndTheArgumentsTypedAndNothingElse() {
        final @NotNull AgentSettingsConfigurable page = new AgentSettingsConfigurable();
        final @NotNull JComponent shown = page.createComponent();
        page.reset();
        assertEquals(Bundle.message("agent.page.name"), page.getDisplayName());
        assertEquals("the page does not show the stored connection", List.of("claude", "-p"), fieldsOn(shown).stream().map(JBTextField::getText).toList());
        assertFalse("the stored connection, shown as it is, counts as a change", page.isModified());

        fieldsOn(shown).getLast().setText(" exec ");
        assertTrue("typed arguments alone do not count as a change", page.isModified());

        fieldsOn(shown).getFirst().setText("  codex ");
        fieldsOn(shown).getLast().setText(" exec ");
        assertTrue("a typed command does not count as a change", page.isModified());

        page.apply();
        assertEquals("the command was not stored as typed, without its spaces", "codex", settings().agentCommand);
        assertEquals("the arguments were not stored as typed, without their spaces", "exec", settings().agentArguments);
        assertFalse("the page still counts a change after it was applied", page.isModified());
    }

    // UC-CODEGEN-021, Rule-CODEGEN-085
    public void testCheckWithNoCommandAsksForOneAndResetClearsWhatItSaid() {
        final @NotNull AgentSettingsConfigurable page = new AgentSettingsConfigurable();
        final @NotNull JComponent shown = page.createComponent();
        fieldsOn(shown).getFirst().setText("");

        pressed(shown, Bundle.message("agent.check.button"));
        assertTrue("Check with no command did not ask for one", Drawn.words(shown).contains(Bundle.message("agent.check.no.command")));

        page.reset();
        assertFalse("Reset kept what Check said", Drawn.words(shown).contains(Bundle.message("agent.check.no.command")));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    public void testThePromptTestinShipsIsStoredAsEmptySoALaterReleaseCanImproveIt() {
        final @NotNull AgentPromptConfigurable page = new AgentPromptConfigurable();
        final @NotNull JComponent shown = page.createComponent();
        page.reset();
        assertEquals(Bundle.message("agent.prompt.page.name"), page.getDisplayName());
        assertEquals("an empty stored prompt does not show the one Testin ships", Bundle.message("agent.prompt.default"), promptOn(shown).getText());
        assertFalse("the prompt Testin ships, shown as it is, counts as a change", page.isModified());

        promptOn(shown).setText("Write {method} in {class}.");
        assertTrue("a prompt the tester typed does not count as a change", page.isModified());
        page.apply();
        assertEquals("the prompt the tester typed was not stored", "Write {method} in {class}.", settings().agentPrompt);

        pressed(shown, Bundle.message("agent.reset.button"));
        assertEquals("Reset did not put back the prompt Testin ships", Bundle.message("agent.prompt.default"), promptOn(shown).getText());
        page.apply();
        assertEquals("the prompt Testin ships was stored instead of an empty prompt", "", settings().agentPrompt);
    }
}
