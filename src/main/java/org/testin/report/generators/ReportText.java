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

package org.testin.report.generators;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportText {
    public static final @NotNull String PLUGIN_NAME = "Testin";
    public static final @NotNull String PLUGIN_URL = "https://plugins.jetbrains.com/plugin/31514-testin";

    public static @NotNull String footerLead() {
        return Bundle.message("report.footer.prefix") + " ";
    }

    public static @NotNull String joined(final @NotNull String separator, final String @NotNull ... parts) {
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(separator));
    }
}
