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

package org.testin.robot;

import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChange;
import org.testin.codegen.CodeChangeRegistry;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.writer.CodeChangeBinding;
import org.testin.config.AutomationLanguage;
import org.testin.model.node.AbstractNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RobotCodeChangeRegistry implements CodeChangeRegistry {
    private final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> bindings = byType();

    // Rule-CODEGEN-128
    private static @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType() {
        final @NotNull Map<CodeChangeType<?>, CodeChangeBinding<?>> byType = new HashMap<>();

        CodeChangeBinding.bind(byType, CodeChangeType.RENAME_TEST_PROJECT, RobotFolders::renameFolder);
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_PROJECT, RobotFolders::removeFolder);
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_SET_PACKAGE, RobotFolders::removeFolder);
        CodeChangeBinding.bind(byType, CodeChangeType.RENAME_TEST_SET_PACKAGE, RobotFolders::renameFolder);
        CodeChangeBinding.bind(byType, CodeChangeType.MOVE_TEST_SET_PACKAGE, RobotFolders::moveFolder);
        CodeChangeBinding.bind(byType, CodeChangeType.CREATE_TEST_SET, RobotFolders::createSuite);
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_SET, RobotFolders::removeSuite);
        CodeChangeBinding.bind(byType, CodeChangeType.RENAME_TEST_SET, RobotFolders::renameSuite);
        CodeChangeBinding.bind(byType, CodeChangeType.MOVE_TEST_SET, RobotFolders::moveSuite);
        CodeChangeBinding.bind(byType, CodeChangeType.CREATE_TEST_CASE, RobotTestChanges::create);
        CodeChangeBinding.bind(byType, CodeChangeType.REMOVE_TEST_CASE, RobotTestChanges::remove);
        CodeChangeBinding.bind(byType, CodeChangeType.MOVE_TEST_CASE, RobotTestChanges::move);
        CodeChangeBinding.bind(byType, CodeChangeType.COPY_TEST_CASE, RobotTestChanges::copy);
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION, RobotTestChanges::describe);
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_GROUP, (project, testCase) -> RobotTestChanges.retag(project, testCase, CodeChangeType.UPDATE_TEST_CASE_GROUP));
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_STATUS, (project, testCase) -> RobotTestChanges.retag(project, testCase, CodeChangeType.UPDATE_TEST_CASE_STATUS));
        CodeChangeBinding.bind(byType, CodeChangeType.UPDATE_TEST_CASE_ORDER, RobotTestChanges::arrange);
        CodeChangeBinding.bind(byType, CodeChangeType.RECONCILE_TEST_CASE, RobotTestChanges::reconcile);

        return byType;
    }

    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.ROBOT_FRAMEWORK;
    }

    // Rule-CODEGEN-128
    @Override
    public <T> @NotNull CodeChange<T> actionFor(final @NotNull CodeChangeType<T> type) {
        return CodeChangeBinding.actionFor(bindings, type);
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public @NotNull Optional<String> folderOf(final @NotNull AbstractNode node) {
        return RobotNames.folderOf(node.treePath());
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public @NotNull Optional<String> fileOf(final @NotNull AbstractNode testSet) {
        return RobotNames.fileOf(testSet.treePath());
    }
}
