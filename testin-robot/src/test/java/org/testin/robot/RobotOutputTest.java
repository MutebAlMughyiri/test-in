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

package org.testin.robot;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.AutomationExecutionStatus;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class RobotOutputTest {

    private static final @NotNull UUID SIGN_IN = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final @NotNull UUID WRONG_PASSWORD = UUID.fromString("11111111-1111-4111-8111-111111111102");

    private static final @NotNull UUID LOCKED = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static final @NotNull String OUTPUT = """
            <robot generator="Robot 7.5 (Python 3.14.7 on win32)" schemaversion="5">
            <suite id="s1" name="Login">
            <test id="s1-t1" name="Sign in with a correct username and password" line="2">
            <kw name="No Operation" owner="BuiltIn"><status status="PASS" start="2026-10-20T05:00:00.000000" elapsed="0.000"/></kw>
            <tag>11111111-1111-4111-8111-111111111101</tag>
            <tag>FUNCTIONAL</tag>
            <status status="PASS" start="2026-10-20T05:00:00.000000" elapsed="0.012"/>
            </test>
            <test id="s1-t2" name="Refuse a sign-in with the wrong password" line="7">
            <tag>11111111-1111-4111-8111-111111111102</tag>
            <tag>A-SECURITY</tag>
            <status status="FAIL" start="2026-10-20T05:00:00.020000" elapsed="1.5">Welcome != Refused</status>
            </test>
            <test id="s1-t3" name="Lock the account after five refusals" line="12">
            <tag>11111111-1111-4111-8111-111111111103</tag>
            <status status="SKIP" start="2026-10-20T05:00:01.600000" elapsed="0.000"/>
            </test>
            </suite>
            </robot>
            """;

    private static @NotNull List<RobotTestExecution> executionsOf(final @NotNull List<UUID> wanted) {
        try {
            final @NotNull Element robot = JDOMUtil.load(OUTPUT);
            return RobotOutput.executionsIn(robot, wanted);
        } catch (final Exception ex) {
            throw new AssertionError("the sample output.xml did not parse: " + ex.getMessage(), ex);
        }
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-120
    @Test
    public void eachTestIsAnExecutionOfTheTestCaseItsUuidTagNames() {
        final @NotNull List<RobotTestExecution> executions = executionsOf(List.of(SIGN_IN, WRONG_PASSWORD, LOCKED));

        assertEquals(executions.stream().map(RobotTestExecution::testCaseUuid).toList(), List.of(SIGN_IN, WRONG_PASSWORD, LOCKED), "Robot sorts tags, so the UUID is found among them, not by place");
        assertEquals(executions.stream().map(RobotTestExecution::automationExecutionStatus).toList(), List.of(AutomationExecutionStatus.PASSED, AutomationExecutionStatus.FAILED, AutomationExecutionStatus.SKIPPED));
        assertEquals(executions.get(1).duration(), Duration.ofMillis(1500), "the test's own elapsed seconds, not a keyword's");
        assertEquals(executions.get(1).failure().message(), "Welcome != Refused", "a failure carries Robot's message");
        assertEquals(executions.get(2).failure().message(), "Skipped/Terminated", "a skip with no message reads as Rule-CODEGEN-075 says");
    }

    // Rule-CODEGEN-120
    @Test
    public void aTestNobodyAskedForIsNoExecution() {
        assertEquals(executionsOf(List.of(WRONG_PASSWORD)).stream().map(RobotTestExecution::testCaseUuid).toList(), List.of(WRONG_PASSWORD));
    }
}
