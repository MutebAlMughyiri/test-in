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


package org.testin.help;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

// UC-INTERNAL-009, Rule-INTERNAL-127
@Getter
@AllArgsConstructor
public enum SetupStep {
    TESTIN_FOLDER(
            Guide.SET_UP_THIS_MACHINE
    ),

    TEST_PROJECT_LINK(
            Guide.LINK_THIS_REPOSITORY
    ),

    BUG_FILING(
            Guide.RAISE_BUG_REPORTS
    ),

    BUG_STATES(
            Guide.RAISE_BUG_REPORTS
    ),

    BOARD_COLUMNS(
            Guide.RAISE_BUG_REPORTS
    ),

    JAVA_PLUGIN(
            Guide.AUTOMATION_CODE
    ),

    TESTNG_PLUGIN(
            Guide.AUTOMATION_CODE
    ),

    TEST_SOURCE_FOLDER(
            Guide.AUTOMATION_CODE
    ),

    TERMINAL_PLUGIN(
            Guide.AUTOMATION_CODE
    ),

    GIT_PLUGIN(
            Guide.SHARE_OVER_GIT
    ),

    GIT_REPOSITORY(
            Guide.SHARE_OVER_GIT
    ),

    GIT_REMOTE(
            Guide.SHARE_OVER_GIT
    );

    private final @NotNull Guide guide;
}
