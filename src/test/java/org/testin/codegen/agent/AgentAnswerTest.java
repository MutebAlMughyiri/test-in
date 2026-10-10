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

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AgentAnswerTest {

    private static final String STATEMENTS = "driver.get(\"https://nafath.sa\");\nAssert.assertTrue(true);";

    @Test
    public void bareStatementsAreTakenAsTheyAre() {
        assertEquals(AgentAnswer.statementsIn(STATEMENTS), Optional.of(STATEMENTS));
    }

    @Test
    public void aFencedBlockIsUnwrapped() {
        final String fenced = """
                Here you go:
                ```java
                %s
                ```
                Hope that helps.""".formatted(STATEMENTS);

        assertEquals(AgentAnswer.statementsIn(fenced), Optional.of(STATEMENTS),
                "the prose around a fenced block would not compile, and the tester never asked for it");
    }

    // Rule-CODEGEN-088
    @Test
    public void theLargestBlockWinsWhenAnAgentShowsSeveral() {
        final String two = """
                ```java
                int x = 1;
                ```
                and the real one:
                ```java
                %s
                ```""".formatted(STATEMENTS);

        assertEquals(AgentAnswer.statementsIn(two), Optional.of(STATEMENTS));
    }

    // Rule-CODEGEN-088
    @Test
    public void anApologyIsNotABody() {
        assertTrue(AgentAnswer.statementsIn("I am sorry, I cannot help with that").isEmpty(),
                "an answer that is not Java would leave a method that does not compile");
    }

    @Test
    public void nothingAtAllIsNotABody() {
        assertTrue(AgentAnswer.statementsIn("   ").isEmpty());
    }
}
