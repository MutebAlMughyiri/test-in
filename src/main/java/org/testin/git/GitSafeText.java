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

package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitSafeText {
    private static final @NotNull Pattern CREDENTIALS = Pattern.compile("([a-zA-Z][a-zA-Z0-9+.\\-]*://)[^/@\\s]+@");

    // UC-SHARE-013, Rule-SHARE-062
    public static @NotNull String withoutCredentials(final @NotNull String text) {
        return CREDENTIALS.matcher(text).replaceAll("$1***@");
    }
}
