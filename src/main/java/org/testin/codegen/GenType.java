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

package org.testin.codegen;

import com.google.errorprone.annotations.Immutable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.event.CodeUpdate;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.MovedTestCase;
import org.testin.codegen.event.NoOpCodeUpdate;
import org.testin.codegen.event.RenamedNode;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;

import java.util.List;
import java.util.UUID;

@Immutable
public record GenType<T>(@NotNull Class<T> payload, @NotNull String description, @NotNull CodeUpdate update) {
    public static final @NotNull GenType<AbstractNode> REMOVE_TEST_PROJECT = new GenType<>(
            AbstractNode.class,
            Bundle.message("codegen.remove.test.project"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<RenamedNode> RENAME_TEST_PROJECT = new GenType<>(
            RenamedNode.class,
            Bundle.message("codegen.rename.test.project"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<AbstractNode> REMOVE_TEST_SET_PACKAGE = new GenType<>(
            AbstractNode.class,
            Bundle.message("codegen.remove.test.set.package"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<RenamedNode> RENAME_TEST_SET_PACKAGE = new GenType<>(
            RenamedNode.class,
            Bundle.message("codegen.rename.test.set.package"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<MovedNode> MOVE_TEST_SET_PACKAGE = new GenType<>(
            MovedNode.class,
            Bundle.message("codegen.move.test.set.package"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<AbstractNode> CREATE_TEST_SET = new GenType<>(
            AbstractNode.class,
            Bundle.message("codegen.create.test.set"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<AbstractNode> REMOVE_TEST_SET = new GenType<>(
            AbstractNode.class,
            Bundle.message("codegen.remove.test.set"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<RenamedNode> RENAME_TEST_SET = new GenType<>(
            RenamedNode.class,
            Bundle.message("codegen.rename.test.set"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<MovedNode> MOVE_TEST_SET = new GenType<>(
            MovedNode.class,
            Bundle.message("codegen.move.test.set"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> CREATE_TEST_CASE = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.create.test.case"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> REMOVE_TEST_CASE = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.remove.test.case"),
            GeneratedCodeUpdate.INSTANCE
    );

    // UC-CODEGEN-002, Rule-CODEGEN-077
    public static final @NotNull GenType<MovedTestCase> MOVE_TEST_CASE = new GenType<>(
            MovedTestCase.class,
            Bundle.message("codegen.move.test.case"),
            GeneratedCodeUpdate.INSTANCE
    );

    // UC-CODEGEN-002, Rule-CODEGEN-078
    public static final @NotNull GenType<CopiedTestCase> COPY_TEST_CASE = new GenType<>(
            CopiedTestCase.class,
            Bundle.message("codegen.copy.test.case"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_DESCRIPTION = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case.description"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_EXPECTED_RESULT = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case"),
            new NoOpCodeUpdate("expected result")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_MODULE = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case"),
            new NoOpCodeUpdate("module")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_TEST_DATA = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case"),
            new NoOpCodeUpdate("test data")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_PRE_CONDITIONS = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case"),
            new NoOpCodeUpdate("pre-conditions")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_STEPS = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case"),
            new NoOpCodeUpdate("steps")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_GROUP = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case.group"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_PRIORITY = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case"),
            new NoOpCodeUpdate("priority")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_ORDER = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case.order"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> RECONCILE_TEST_CASE = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.restore.test.case"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_STATUS = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.update.test.case.status"),
            GeneratedCodeUpdate.INSTANCE
    );

    public static final @NotNull GenType<TestCaseDto> NO_CODE_CHANGE = new GenType<>(
            TestCaseDto.class,
            Bundle.message("codegen.no.code.change"),
            new NoOpCodeUpdate("read-only attribute")
    );

    // UC-CODEGEN-019, Rule-CODEGEN-005
    public void execute(final @NotNull Project project, final @NotNull T payload) {
        update.execute(this, project, payload);
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-EDITOR-PANEL-046
    public void executeAll(final @NotNull Project project, final @NotNull List<? extends T> payloads) {
        ApplicationManager.getApplication().invokeLater(() -> executeAllNow(project, payloads), project.getDisposed());
    }

    public void executeAllNow(final @NotNull Project project, final @NotNull List<? extends T> payloads) {
        executeAllNow(project, payloads, UUID.randomUUID().toString());
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-018
    public void executeAllNow(final @NotNull Project project, final @NotNull List<? extends T> payloads, final @NotNull String undoGroup) {
        update.executeAll(this, project, payloads, undoGroup);
    }

    @Override
    public @NotNull String toString() {
        return description;
    }
}
