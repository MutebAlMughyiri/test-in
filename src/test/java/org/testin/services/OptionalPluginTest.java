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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class OptionalPluginTest {

    // Rule-CODEGEN-062
    @Test
    public void anEntryThatWaitsForThePluginNamesIt() {
        assertEquals(OptionalPlugin.JAVA.needs("Automate Test Case"), "Automate Test Case (needs the Java plugin)",
                "an entry that is gray without saying which plugin it waits for teaches the tester nothing");
    }
}
