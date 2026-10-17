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

package org.testin.java.codegen;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeRegistry;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.NoCodeChange;
import org.testin.java.codegen.clazz.CreateJavaClass;
import org.testin.java.codegen.clazz.MoveJavaClass;
import org.testin.java.codegen.clazz.RemoveJavaClass;
import org.testin.java.codegen.clazz.RenameJavaClass;
import org.testin.java.codegen.method.CopyTestMethod;
import org.testin.java.codegen.method.CreateTestMethod;
import org.testin.java.codegen.method.MoveTestMethod;
import org.testin.java.codegen.method.RemoveTestMethod;
import org.testin.java.codegen.method.update.ReconcileTestMethod;
import org.testin.java.codegen.method.update.UpdateTestMethodDescription;
import org.testin.java.codegen.method.update.UpdateTestMethodEnabled;
import org.testin.java.codegen.method.update.UpdateTestMethodGroups;
import org.testin.java.codegen.method.update.UpdateTestMethodOrder;
import org.testin.java.codegen.pkg.MoveJavaPackage;
import org.testin.java.codegen.pkg.RemoveJavaPackage;
import org.testin.java.codegen.pkg.RenameJavaPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
public final class JavaCodeChangeRegistry implements CodeChangeRegistry {
    private final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> bindings = byType();

    private static @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType() {
        final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType = new HashMap<>();

        put(byType, CodeChangeType.RENAME_TEST_PROJECT, new RenameJavaPackage());
        put(byType, CodeChangeType.REMOVE_TEST_PROJECT, new RemoveJavaPackage());
        put(byType, CodeChangeType.REMOVE_TEST_SET_PACKAGE, new RemoveJavaPackage());
        put(byType, CodeChangeType.RENAME_TEST_SET_PACKAGE, new RenameJavaPackage());
        put(byType, CodeChangeType.MOVE_TEST_SET_PACKAGE, new MoveJavaPackage());
        put(byType, CodeChangeType.CREATE_TEST_SET, new CreateJavaClass());
        put(byType, CodeChangeType.REMOVE_TEST_SET, new RemoveJavaClass());
        put(byType, CodeChangeType.RENAME_TEST_SET, new RenameJavaClass());
        put(byType, CodeChangeType.MOVE_TEST_SET, new MoveJavaClass());
        put(byType, CodeChangeType.CREATE_TEST_CASE, new CreateTestMethod());
        put(byType, CodeChangeType.REMOVE_TEST_CASE, new RemoveTestMethod());
        put(byType, CodeChangeType.MOVE_TEST_CASE, new MoveTestMethod());
        put(byType, CodeChangeType.COPY_TEST_CASE, new CopyTestMethod());
        put(byType, CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestMethodDescription());
        put(byType, CodeChangeType.UPDATE_TEST_CASE_GROUP, new UpdateTestMethodGroups());
        put(byType, CodeChangeType.UPDATE_TEST_CASE_ORDER, new UpdateTestMethodOrder());
        put(byType, CodeChangeType.UPDATE_TEST_CASE_STATUS, new UpdateTestMethodEnabled());
        put(byType, CodeChangeType.RECONCILE_TEST_CASE, new ReconcileTestMethod());

        return byType;
    }

    private static <T> void put(final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType, final @NotNull CodeChangeType<T> type, final @NotNull CodeChange<T> action) {
        byType.put(type, new CodeChangeBinding<>(type, action));
    }

    @Override
    public <T> @NotNull CodeChange<T> actionFor(final @NotNull CodeChangeType<T> type) {
        return Optional.ofNullable(bindings.get(type))
                .<CodeChange<T>>map(CodeChangeBinding::typed)
                .orElseGet(() -> new NoCodeChange<>(type.description()));
    }
}
