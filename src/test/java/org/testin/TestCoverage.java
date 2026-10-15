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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCoverage {
    private static final @NotNull String FOLDER = "testin.testCoverage";

    private static final @NotNull String AGENT = "org.jacoco:type=Runtime";

    public static void save(final @NotNull Class<?> testClass, final @NotNull String testMethod) {
        final @NotNull String folder = System.getProperty(FOLDER, "");
        if (folder.isEmpty()) return;

        try {
            final @NotNull MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final @NotNull ObjectName agent = new ObjectName(AGENT);
            if (!server.isRegistered(agent)) return;

            final byte @NotNull [] ran = (byte[]) server.invoke(agent, "getExecutionData", new Object[]{true}, new String[]{"boolean"});
            final @NotNull Path saved = Files.createDirectories(Path.of(folder));
            Files.write(saved.resolve(declaringClassOf(testClass, testMethod) + "#" + testMethod + ".exec"), ran, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final Exception ex) {
            TestinLogger.warn("Could not save what " + testClass.getName() + "#" + testMethod + " ran: " + ex.getMessage());
        }
    }

    private static @NotNull String declaringClassOf(final @NotNull Class<?> testClass, final @NotNull String testMethod) {
        return Arrays.stream(testClass.getMethods())
                .filter(method -> method.getName().equals(testMethod))
                .findFirst()
                .map(method -> method.getDeclaringClass().getName())
                .orElse(testClass.getName());
    }
}
