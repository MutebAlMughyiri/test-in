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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.TestRunConfiguration;
import org.testng.annotations.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRunFieldVisibilityTest {

    private static final @NotNull String WEB = "Web";
    private static final @NotNull String MOBILE = "Mobile";
    private static final @NotNull String FRONTEND = "Frontend";
    private static final @NotNull String BACKEND = "Backend";

    private static @NotNull TestRunConfiguration.Chosen answers(final @NotNull String platform, final @NotNull String component) {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        answers.put(TestRunConfiguration.PLATFORM, platform);
        answers.put(TestRunConfiguration.COMPONENT, component);

        return field -> answers.getOrDefault(field, "");
    }

    @Test
    public void aWebFrontendIsAskedWhichBrowser() {
        assertTrue(TestRunConfiguration.BROWSER.isShownFor(answers(WEB, FRONTEND)));
    }

    @Test
    public void aMobileFrontendIsAskedWhichDevice() {
        assertTrue(TestRunConfiguration.DEVICE_TYPE.isShownFor(answers(MOBILE, FRONTEND)));
    }

    @Test
    public void aTestRunIsNeverAskedForBothAtOnce() {
        for (final String platform : List.of(WEB, MOBILE, BACKEND, "")) {
            for (final String component : List.of(FRONTEND, BACKEND, "")) {
                final @NotNull TestRunConfiguration.Chosen chosen = answers(platform, component);

                assertFalse(TestRunConfiguration.BROWSER.isShownFor(chosen)
                                && TestRunConfiguration.DEVICE_TYPE.isShownFor(chosen),
                        "both were asked for a " + platform + " " + component + " test run");
            }
        }
    }

    @Test
    public void aBackendTestRunIsAskedForNeither() {
        assertFalse(TestRunConfiguration.BROWSER.isShownFor(answers(WEB, BACKEND)),
                "a backend has no browser");
        assertFalse(TestRunConfiguration.DEVICE_TYPE.isShownFor(answers(MOBILE, BACKEND)),
                "a backend has no handset");
    }

    @Test
    public void nothingChosenYetMeansNeitherIsAskedFor() {
        assertFalse(TestRunConfiguration.BROWSER.isShownFor(answers("", "")));
        assertFalse(TestRunConfiguration.DEVICE_TYPE.isShownFor(answers("", "")));
    }

    @Test
    public void everyOtherFieldIsOnEveryTestRun() {
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field == TestRunConfiguration.BROWSER || field == TestRunConfiguration.DEVICE_TYPE) continue;

            assertTrue(field.isShownFor(answers("", "")),
                    field.getDisplayName() + " depends on an answer but nothing says when it applies");
        }
    }

    @Test
    public void theRulesLookForAnswersTheListsOffer() {
        final @NotNull List<String> platforms = TestRunConfiguration.PLATFORM.getOptions();
        final @NotNull List<String> components = TestRunConfiguration.COMPONENT.getOptions();

        assertTrue(platforms.contains(WEB) && platforms.contains(MOBILE),
                "the platform list must still offer " + WEB + " and " + MOBILE + ", it offers " + platforms);
        assertTrue(components.contains(FRONTEND),
                "the component list must still offer " + FRONTEND + ", it offers " + components);
    }

    @Test
    public void theDeviceListOffersTheThreeHandsets() {
        assertEquals(TestRunConfiguration.DEVICE_TYPE.getOptions(),
                List.of("iPhone", "Samsung", "Huawei"));
    }
}
