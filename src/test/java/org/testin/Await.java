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

package org.testin;

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Await {
    private static final long TIMEOUT_MILLIS = 60_000;

    public static void until(final @NotNull String failure, final @NotNull BooleanSupplier landed) {
        final long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline) {
            if (landed.getAsBoolean()) return;

            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }

        throw new AssertionError(failure);
    }
}
