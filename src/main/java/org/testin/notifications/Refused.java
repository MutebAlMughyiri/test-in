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

package org.testin.notifications;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.util.Bundle;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum Refused {
    // Rule-TREE-PANEL-004
    ALREADY_EXISTS(
            Bundle.message("refused.already.exists")
    ),

    // UC-CODEGEN-017, Rule-CODEGEN-080
    PACKAGE_TAKEN(
            Bundle.message("refused.package.taken")
    ),

    NO_GENERATED_CODE(
            Bundle.message("refused.no.generated.code")
    ),

    // UC-CODEGEN-008, Rule-CODEGEN-074
    NO_GENERATED_CODE_COUNTED(
            Bundle.message("refused.no.generated.code.counted")
    ),

    // UC-CODEGEN-008, Rule-CODEGEN-097
    ACROSS_MODULES(
            Bundle.message("refused.across.modules")
    ),

    // UC-TREE-PANEL-023, Rule-TREE-PANEL-078
    NOTHING_TO_RUN(
            Bundle.message("refused.nothing.to.run")
    ),

    NOTHING_SHOWING(
            Bundle.message("refused.nothing.showing")
    ),

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009
    HIDDEN_BY_THE_FILTER(
            Bundle.message("refused.hidden.by.the.filter")
    ),

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-004
    WHILE_INDEXING(
            Bundle.message("refused.while.indexing")
    ),

    // UC-SHARE-013, Rule-SHARE-060
    NOT_A_REPOSITORY_URL(
            Bundle.message("refused.not.a.repository.url")
    ),

    // UC-SHARE-014, Rule-SHARE-108
    NOT_AN_EMAIL_ADDRESS(
            Bundle.message("refused.not.an.email.address")
    ),

    // UC-TREE-PANEL-008, Rule-TREE-PANEL-095, Rule-CODEGEN-073
    NOT_A_JAVA_NAME(
            Bundle.message("refused.not.a.java.name")
    ),

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, Rule-TREE-PANEL-095
    NOT_ONE_FOLDER(
            Bundle.message("refused.not.one.folder")
    ),

    // UC-CODEGEN-007, Rule-CODEGEN-069
    NO_TEST_CASE_BEHIND_IT(
            Bundle.message("refused.no.test.case.behind.it")
    ),

    // UC-CODEGEN-022, Rule-CODEGEN-099
    NOTHING_PASTED(
            Bundle.message("refused.nothing.pasted")
    ),

    // UC-CODEGEN-022, Rule-CODEGEN-099
    NOT_JSON(
            Bundle.message("refused.not.json")
    ),

    // UC-CODEGEN-022, Rule-CODEGEN-099
    NOT_A_JSON_OBJECT(
            Bundle.message("refused.not.a.json.object")
    ),

    // UC-CODEGEN-022, Rule-CODEGEN-099
    NO_KEYS(
            Bundle.message("refused.no.keys")
    ),

    // UC-EDITOR-PANEL-008, UC-SHARE-006, Rule-EDITOR-PANEL-206, Rule-SHARE-106
    UNREADABLE(
            Bundle.message("refused.unreadable")
    ),

    // UC-REPORT-001, UC-SHARE-001, Rule-REPORT-004, Rule-SHARE-010
    INSIDE_THE_TESTIN_FOLDER(
            Bundle.message("refused.inside.the.testin.folder")
    );

    private final @NotNull String sentence;

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, Rule-TREE-PANEL-095
    public static @NotNull Optional<Refused> ofName(final @NotNull NodeType type, final @NotNull String name) {
        if (!NodeType.isOneFolderName(name)) return Optional.of(NOT_ONE_FOLDER);
        if (!type.canTakeName(name)) return Optional.of(NOT_A_JAVA_NAME);

        return Optional.empty();
    }

    public @NotNull String about(final @NotNull String name) {
        return sentence.formatted(name);
    }
}
