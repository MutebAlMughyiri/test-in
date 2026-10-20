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
import org.testin.codegen.Fqcn;
import org.testin.codegen.writer.CodeChangeBinding;
import org.testin.config.AutomationLanguage;
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
import org.testin.model.node.AbstractNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
public final class JavaCodeChangeRegistry implements CodeChangeRegistry {
    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.JAVA;
    }

    private final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> bindings = byType();

    private static @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType() {
        final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType = new HashMap<>();

        CodeChangeBinding.bind(byType, CodeChangeType.RENAME_TEST_PROJECT, new RenameJavaPackage());
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_PROJECT, new RemoveJavaPackage());
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_SET_PACKAGE, new RemoveJavaPackage());
        CodeChangeBinding.bind(byType, CodeChangeType.RENAME_TEST_SET_PACKAGE, new RenameJavaPackage());
        CodeChangeBinding.bind(byType, CodeChangeType.MOVE_TEST_SET_PACKAGE, new MoveJavaPackage());
        CodeChangeBinding.bind(byType, CodeChangeType.CREATE_TEST_SET, new CreateJavaClass());
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_SET, new RemoveJavaClass());
        CodeChangeBinding.bind(byType, CodeChangeType.RENAME_TEST_SET, new RenameJavaClass());
        CodeChangeBinding.bind(byType, CodeChangeType.MOVE_TEST_SET, new MoveJavaClass());
        CodeChangeBinding.bind(byType, CodeChangeType.CREATE_TEST_CASE, new CreateTestMethod());
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_CASE, new RemoveTestMethod());
        CodeChangeBinding.bind(byType, CodeChangeType.MOVE_TEST_CASE, new MoveTestMethod());
        CodeChangeBinding.bind(byType, CodeChangeType.COPY_TEST_CASE, new CopyTestMethod());
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestMethodDescription());
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_GROUP, new UpdateTestMethodGroups());
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_ORDER, new UpdateTestMethodOrder());
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_STATUS, new UpdateTestMethodEnabled());
        CodeChangeBinding.bind(byType, CodeChangeType.RECONCILE_TEST_CASE, new ReconcileTestMethod());

        return byType;
    }


    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public @NotNull Optional<String> folderOf(final @NotNull AbstractNode node) {
        return Fqcn.packageFolderOf(node);
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public @NotNull Optional<String> fileOf(final @NotNull AbstractNode testSet) {
        return Fqcn.classFileOf(testSet);
    }

    @Override
    public <T> @NotNull CodeChange<T> actionFor(final @NotNull CodeChangeType<T> type) {
        return CodeChangeBinding.actionFor(bindings, type);
    }
}
