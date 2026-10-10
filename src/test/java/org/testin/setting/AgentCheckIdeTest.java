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

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.codegen.agent.AgentCli;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class AgentCheckIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull String checked(final @NotNull String command) {
        final @NotNull AgentSettingsConfigurable page = new AgentSettingsConfigurable();
        final @NotNull JComponent shown = page.createComponent();
        final @NotNull List<JBTextField> fields = Drawn.components(shown).stream().filter(JBTextField.class::isInstance).map(JBTextField.class::cast).toList();
        fields.getFirst().setText(command);

        final @NotNull List<String> before = Drawn.words(shown);
        Drawn.components(shown).stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(button -> Bundle.message("agent.check.button").equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the page has no Check button"))
                .doClick();

        return Drawn.words(shown).stream().filter(word -> !before.contains(word)).findFirst().orElse("");
    }

    private @NotNull Path aFakeAgent() {
        try {
            if (SystemInfo.isWindows) {
                final @NotNull Path agent = root.resolve("fake-agent.cmd");
                Files.writeString(agent, "@echo off\r\necho fake-agent %1\r\necho a second line\r\n", StandardCharsets.US_ASCII);
                return agent;
            }

            final @NotNull Path agent = root.resolve("fake-agent.sh");
            Files.writeString(agent, """
                    #!/bin/sh
                    echo "fake-agent $1"
                    echo a second line
                    """, StandardCharsets.US_ASCII);
            if (!agent.toFile().setExecutable(true))
                throw new AssertionError("could not make the stand-in agent runnable");
            return agent;
        } catch (final IOException ex) {
            throw new AssertionError("could not write the stand-in agent: " + ex.getMessage(), ex);
        }
    }

    // Rule-CODEGEN-085
    public void testCheckRunsTheCommandWithVersionAndPrintsTheFirstLineItAnswered() {
        final @NotNull Path agent = aFakeAgent();

        assertEquals("Check did not print the first line the agent answered to --version", Bundle.message("agent.check.answered", "fake-agent --version"), checked(agent.toString()));
    }

    // Rule-CODEGEN-085
    public void testACommandNothingOnPathAnswersIsRefusedNamingWhatWasLookedFor() {
        final @NotNull String command = "testin-no-such-agent";

        final @NotNull String said = checked(command);

        assertEquals("the refusal does not name the command and the spellings looked for", Bundle.message("agent.check.not.found", command, AgentCli.triedNames(command)), said);
        if (SystemInfo.isWindows) {
            for (final String spelling : List.of(".exe", ".cmd", ".bat"))
                assertTrue("the refusal does not say " + command + spelling + " was looked for: " + said, said.toLowerCase(Locale.ROOT).contains(command + spelling));
        }
    }
}
