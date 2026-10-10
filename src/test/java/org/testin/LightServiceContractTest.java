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

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.fail;

public class LightServiceContractTest {

    private static final @NotNull List<String> ACCEPTED = List.of(
            "",
            "com.intellij.openapi.project.Project",
            "kotlinx.coroutines.CoroutineScope",
            "com.intellij.openapi.project.Project,kotlinx.coroutines.CoroutineScope");

    private static boolean isAccepted(final @NotNull Constructor<?> constructor) {
        return ACCEPTED.contains(signature(constructor));
    }

    private static @NotNull String signature(final @NotNull Constructor<?> constructor) {
        return String.join(",", Arrays.stream(constructor.getParameterTypes()).map(Class::getName).toList());
    }

    private static @NotNull List<Class<?>> findServices() {
        return MainClasses.all().stream().filter(type -> type.isAnnotationPresent(Service.class)).toList();
    }

    @Test
    public void everyLightServiceCanBeBuiltByThePlatform() {
        try {
            final List<Class<?>> services = findServices();
            if (services.isEmpty()) {
                fail("No @Service classes found in src/main/java: the scan is looking in the wrong place");
            }

            Logger.info("Checked " + services.size() + " light services");

            final List<String> breaches = new ArrayList<>();

            for (final Class<?> service : services) {
                if (!Modifier.isFinal(service.getModifiers())) {
                    breaches.add(service.getName() + " is not final");
                }

                if (Arrays.stream(service.getDeclaredConstructors()).noneMatch(LightServiceContractTest::isAccepted)) {
                    breaches.add(service.getName() + " declares no constructor the platform will call, only "
                            + Arrays.stream(service.getDeclaredConstructors())
                            .map(LightServiceContractTest::signature)
                            .toList());
                }
            }

            if (!breaches.isEmpty()) {
                fail("Light services the platform will refuse to build:\n  " + String.join("\n  ", breaches));
            }
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
