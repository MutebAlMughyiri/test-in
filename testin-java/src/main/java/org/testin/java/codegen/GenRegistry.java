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
import org.testin.codegen.CodeGenerators;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.NoJavaCode;
import org.testin.java.codegen.clazz.CreateJavaClass;
import org.testin.java.codegen.clazz.MoveJavaClass;
import org.testin.java.codegen.clazz.RemoveJavaClass;
import org.testin.java.codegen.clazz.RenameJavaClass;
import org.testin.java.codegen.method.CopyTestMethod;
import org.testin.java.codegen.method.CreateTestMethod;
import org.testin.java.codegen.method.MoveTestMethod;
import org.testin.java.codegen.method.RemoveTestMethod;
import org.testin.java.codegen.method.update.ReconcileTestMethod;
import org.testin.java.codegen.method.update.UpdateTestDescription;
import org.testin.java.codegen.method.update.UpdateTestEnabled;
import org.testin.java.codegen.method.update.UpdateTestGroup;
import org.testin.java.codegen.method.update.UpdateTestOrder;
import org.testin.java.codegen.pkg.MoveJavaPackage;
import org.testin.java.codegen.pkg.RemoveJavaPackage;
import org.testin.java.codegen.pkg.RenameJavaPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
public final class GenRegistry implements CodeGenerators {
    private final @NotNull Map<GenType<?>, GenBinding<?>> bindings = byType();

    private static @NotNull Map<GenType<?>, GenBinding<?>> byType() {
        final @NotNull Map<GenType<?>, GenBinding<?>> byType = new HashMap<>();

        put(byType, GenType.RENAME_TEST_PROJECT, new RenameJavaPackage());
        put(byType, GenType.REMOVE_TEST_PROJECT, new RemoveJavaPackage());
        put(byType, GenType.REMOVE_TEST_SET_PACKAGE, new RemoveJavaPackage());
        put(byType, GenType.RENAME_TEST_SET_PACKAGE, new RenameJavaPackage());
        put(byType, GenType.MOVE_TEST_SET_PACKAGE, new MoveJavaPackage());
        put(byType, GenType.CREATE_TEST_SET, new CreateJavaClass());
        put(byType, GenType.REMOVE_TEST_SET, new RemoveJavaClass());
        put(byType, GenType.RENAME_TEST_SET, new RenameJavaClass());
        put(byType, GenType.MOVE_TEST_SET, new MoveJavaClass());
        put(byType, GenType.CREATE_TEST_CASE, new CreateTestMethod());
        put(byType, GenType.REMOVE_TEST_CASE, new RemoveTestMethod());
        put(byType, GenType.MOVE_TEST_CASE, new MoveTestMethod());
        put(byType, GenType.COPY_TEST_CASE, new CopyTestMethod());
        put(byType, GenType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestDescription());
        put(byType, GenType.UPDATE_TEST_CASE_GROUP, new UpdateTestGroup());
        put(byType, GenType.UPDATE_TEST_CASE_ORDER, new UpdateTestOrder());
        put(byType, GenType.UPDATE_TEST_CASE_STATUS, new UpdateTestEnabled());
        put(byType, GenType.RECONCILE_TEST_CASE, new ReconcileTestMethod());

        return byType;
    }

    private static <T> void put(final @NotNull Map<GenType<?>, GenBinding<?>> byType, final @NotNull GenType<T> type, final @NotNull GenAction<T> action) {
        byType.put(type, new GenBinding<>(type, action));
    }

    @Override
    public <T> @NotNull GenAction<T> actionFor(final @NotNull GenType<T> type) {
        return Optional.ofNullable(bindings.get(type))
                .<GenAction<T>>map(GenBinding::typed)
                .orElseGet(() -> new NoJavaCode<>(type.description()));
    }
}
