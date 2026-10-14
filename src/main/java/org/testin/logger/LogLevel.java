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

package org.testin.logger;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AllArgsConstructor
public enum LogLevel {
    DISABLED(
            -1,
            "OFF  "
    ),

    TRACE(
            0,
            "TRACE"
    ),

    DEBUG(
            1,
            "DEBUG"
    ),

    INFO(
            2,
            "INFO "
    ),

    WARN(
            3,
            "WARN "
    ),

    ERROR(
            4,
            "ERROR"
    ),

    FATAL(
            5,
            "FATAL"
    );

    public final int priority;

    public final @NotNull String paddedName;

    // UC-SETTING-007, Rule-SETTING-025
    public static @NotNull String known(final @NotNull String stored) {
        return Arrays.stream(values()).map(LogLevel::name).filter(stored::equals).findFirst().orElse(INFO.name());
    }
}
