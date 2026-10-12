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

import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.testng.Assert.fail;

public class EnumNullContractTest {

    private static @NotNull List<Class<?>> findEnums() {
        return MainClasses.all().stream().filter(Class::isEnum).toList();
    }

    private static boolean isNullContractBreach(final @NotNull Throwable cause) {
        return cause instanceof IllegalArgumentException
                && cause.getMessage() != null
                && cause.getMessage().contains("must not be null");
    }

    private static @NotNull Throwable rootCause(final @NotNull Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && !current.equals(current.getCause())) current = current.getCause();
        return current;
    }

    @Test
    public void everyEnumConstantHonoursItsNullContract() {
        try {
            final List<String> breaches = new ArrayList<>();
            final List<String> skipped = new ArrayList<>();

            final List<Class<?>> enums = findEnums();
            if (enums.isEmpty())
                fail("No enums found in src/main/java: the scan is looking in the wrong place");
            TestinLogger.info("Checked " + enums.size() + " enums");

            for (final Class<?> type : enums) {
                try {
                    Objects.requireNonNull(type.getEnumConstants(), type.getName() + " is not an enum");
                } catch (final Throwable t) {
                    final Throwable cause = rootCause(t);
                    if (isNullContractBreach(cause)) breaches.add(type.getName() + ": " + cause.getMessage());
                    else skipped.add(type.getName() + ": " + cause);
                }
            }

            if (!skipped.isEmpty()) {
                TestinLogger.info("Enums that could not initialize for reasons unrelated to nullability:");
                skipped.forEach(s -> TestinLogger.info("  " + s));
            }

            if (!breaches.isEmpty()) {
                fail("An enum constant passes null to a field annotated @NotNull. Give the constant a value, or "
                        + "an empty value of the field's own type:\n  " + String.join("\n  ", breaches));
            }
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
