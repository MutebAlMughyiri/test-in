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

package org.testin.codegen;

import org.jetbrains.annotations.NotNull;
import org.testin.codegen.agent.AgentConnection;
import org.testin.setting.AppSettingsState;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class AgentKeyTest {

    private static final @NotNull List<String> SECRET_WORDS = List.of("key", "token", "secret", "password", "credential");

    private static boolean namesASecret(final @NotNull String name) {
        final @NotNull String lower = name.toLowerCase(Locale.ROOT);
        return SECRET_WORDS.stream().anyMatch(lower::contains);
    }

    // Rule-CODEGEN-087
    @Test
    public void testinHasNoPlaceToHoldAKey() {
        final @NotNull List<String> settings = Stream.of(AppSettingsState.class.getDeclaredFields()).map(Field::getName).filter(AgentKeyTest::namesASecret).toList();
        final @NotNull List<String> connection = Stream.of(AgentConnection.class.getRecordComponents()).map(RecordComponent::getName).filter(AgentKeyTest::namesASecret).toList();

        assertEquals(settings, List.of(), "the settings file has a field for a key, which is the agent's to hold, never Testin's");
        assertEquals(connection, List.of(), "the agent connection carries a key onto the command line");

        final @NotNull AgentConnection stored = new AgentConnection("claude", "-p", "Write the steps of this test case");
        assertEquals(List.of(stored.command(), stored.arguments(), stored.prompt()), List.of("claude", "-p", "Write the steps of this test case"), "the agent connection holds something besides the command, its arguments and the prompt");
    }
}
