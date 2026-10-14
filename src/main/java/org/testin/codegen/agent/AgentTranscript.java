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

package org.testin.codegen.agent;

import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;

// UC-CODEGEN-021, Rule-CODEGEN-090
public final class AgentTranscript {
    private final @NotNull List<String> exchanges = new ArrayList<>();

    private int landed;

    // UC-CODEGEN-021, Rule-CODEGEN-090
    public void record(final @NotNull TestCase testCase, final @NotNull String prompt, final @NotNull String said, final boolean bodyLanded, final @NotNull String outcome) {
        if (bodyLanded) landed++;
        TestinLogger.debug("""
                Agent asked for '%s':
                %s
                and said:
                %s""".formatted(testCase.getDescription(), prompt, said));

        exchanges.add(Bundle.message("agent.transcript.exchange", testCase.getDescription(), outcome, prompt, said));
    }

    // Rule-CODEGEN-089
    public int landed() {
        return landed;
    }

    public boolean isEmpty() {
        return exchanges.isEmpty();
    }

    // Rule-CODEGEN-090
    public @NotNull String read() {
        return String.join("\n\n", exchanges);
    }
}
