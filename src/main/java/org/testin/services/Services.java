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

package org.testin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Services {
    private static final @NotNull Map<Class<?>, Boolean> APPLICATION_LEVEL = new ConcurrentHashMap<>();

    public static <T> @NotNull T getInstance(final @NotNull Project project, final @NotNull Class<T> clazz) {
        if (isApplicationLevel(clazz)) return getInstance(clazz);

        return project.getService(clazz);
    }

    public static <T> @NotNull T getInstance(final @NotNull Class<T> clazz) {
        return ApplicationManager.getApplication().getService(clazz);
    }

    static boolean isApplicationLevel(final @NotNull Class<?> clazz) {
        return APPLICATION_LEVEL.computeIfAbsent(clazz, Services::declaresApplicationLevel);
    }

    private static boolean declaresApplicationLevel(final @NotNull Class<?> clazz) {
        final @Nullable Service service = clazz.getAnnotation(Service.class);

        return service != null && Arrays.asList(service.value()).contains(Service.Level.APP);
    }

    public static boolean isNotCreated(final @NotNull Project project, final @NotNull Class<?> clazz) {
        return project.getServiceIfCreated(clazz) == null;
    }
}
