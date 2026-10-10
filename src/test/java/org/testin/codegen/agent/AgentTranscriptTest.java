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
import org.testin.model.TestCaseDto;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AgentTranscriptTest {

    // Rule-CODEGEN-089
    @Test
    public void theCountIsTheBodiesThatLanded() {
        final AgentTranscript transcript = new AgentTranscript();

        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Log in").build(), "prompt", "statements", true, "written");
        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Log out").build(), "prompt", "I am sorry", false, "not Java");
        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Lock").build(), "prompt", "statements", true, "written");

        assertEquals(transcript.landed(), 2);
    }

    // Rule-CODEGEN-090
    @Test
    public void everyExchangeIsKeptInOrderWithTheDroppedOnesMarked() {
        final @NotNull AgentTranscript transcript = new AgentTranscript();

        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Log in").build(), "write log in", "driver.get(url);", true, "written");
        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Log out").build(), "write log out", "I am sorry", false, "not Java");

        final @NotNull String read = transcript.read();
        assertTrue(read.indexOf("write log in") < read.indexOf("driver.get(url);"), "what was asked does not come before what came back");
        assertTrue(read.indexOf("driver.get(url);") < read.indexOf("write log out"), "the exchanges are not in the order the test cases were asked");
        assertTrue(read.contains("Log out - not Java"), "the dropped answer is not marked as dropped: " + read);
        assertTrue(read.contains("I am sorry"), "what the agent said when its answer was dropped was not kept");
    }
}
