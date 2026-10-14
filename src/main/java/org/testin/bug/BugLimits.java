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

package org.testin.bug;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugLimits {
    private static final int TITLE = 256;

    private static final int BODY = 65_536;

    private static final int SCREENSHOTS = 50;

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-071
    static @NotNull Optional<String> whyNot(final @NotNull String title, final @NotNull String body, final int screenshots) {
        final int titleLength = title.strip().length();

        if (titleLength == 0) return Optional.of(Bundle.message("bug.limit.title.empty"));
        if (titleLength > TITLE) return Optional.of(Bundle.message("bug.limit.title.long", titleLength, TITLE));
        if (body.length() > BODY) return Optional.of(Bundle.message("bug.limit.body.long", body.length(), BODY));
        if (screenshots > SCREENSHOTS)
            return Optional.of(Bundle.message("bug.limit.screenshots", screenshots, SCREENSHOTS));

        return Optional.empty();
    }
}
