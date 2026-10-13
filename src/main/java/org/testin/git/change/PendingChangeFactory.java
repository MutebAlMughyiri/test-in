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

package org.testin.git.change;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCaseDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.node.AbstractNode;
import org.testin.model.testrun.RunItem;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PendingChangeFactory {
    // UC-SHARE-010, Rule-SHARE-047
    static @NotNull PendingChange fromFile(final @NotNull StatusKind type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull JsonCodec mapper, final @NotNull Function<UUID, Optional<TestCaseDto>> testCases) {
        return switch (subjectOf(relativePath)) {
            case TEST_CASE -> testCase(type, beforeJson, afterJson, relativePath, mapper);
            case RUN_ITEM -> runItem(type, beforeJson, afterJson, relativePath, mapper, testCases);
            case MARKER -> marker(type, beforeJson, afterJson, relativePath, mapper);
            case OTHER -> byName(ChangeSubject.OTHER, type, relativePath);
        };
    }

    private static @NotNull ChangeSubject subjectOf(final @NotNull Path relativePath) {
        return switch (FileKind.of(relativePath)) {
            case MARKER -> ChangeSubject.MARKER;
            case TEST_CASE -> ChangeSubject.TEST_CASE;
            case RUN_ITEM -> ChangeSubject.RUN_ITEM;
            case SCREENSHOT, OTHER -> ChangeSubject.OTHER;
        };
    }

    private static @NotNull Map<String, Object> fieldsIn(final @NotNull JsonCodec mapper, final @NotNull String json) {
        if (json.isBlank()) return Map.of();

        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (final UncheckedIOException unreadable) {
            return Map.of();
        }
    }

    private static @NotNull PendingChange testCase(final @NotNull StatusKind type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull JsonCodec mapper) {
        final @NotNull String testSet = parentName(relativePath);

        return switch (type) {
            case ADDED -> {
                final @NotNull TestCaseDto newState = read(mapper, afterJson, TestCaseDto.class);
                yield new PendingChange(ChangeSubject.TEST_CASE, newState.getDescription(), testSet,
                        newState.getId().toString(), relativePath, StatusKind.ADDED, nothingCommitted(),
                        List.of(new FieldChange(Bundle.message("caption.test.case"), "", newState.getDescription(), ChangeSubject.TEST_CASE.getCreated())));
            }
            case DELETED -> {
                final @NotNull TestCaseDto oldState = read(mapper, beforeJson, TestCaseDto.class);
                yield new PendingChange(ChangeSubject.TEST_CASE, oldState.getDescription(), testSet,
                        oldState.getId().toString(), relativePath, StatusKind.DELETED, oldState,
                        List.of(new FieldChange(Bundle.message("caption.test.case"), oldState.getDescription(), "", ChangeSubject.TEST_CASE.getRemoved())));
            }
            case MODIFIED -> {
                final @NotNull TestCaseDto oldState = read(mapper, beforeJson, TestCaseDto.class);
                final @NotNull TestCaseDto newState = read(mapper, afterJson, TestCaseDto.class);
                final @NotNull List<FieldChange> fieldChanges = TestCaseFieldChanges.compare(oldState, newState);

                yield new PendingChange(ChangeSubject.TEST_CASE, newState.getDescription(), testSet,
                        newState.getId().toString(), relativePath, StatusKind.MODIFIED, oldState,
                        fieldChanges.isEmpty()
                                ? List.of(new FieldChange(Bundle.message("caption.test.case"), "", Bundle.message("git.change.no.field"), ChangeSubject.TEST_CASE.getChanged()))
                                : fieldChanges);
            }
        };
    }

    // UC-SHARE-010, Rule-SHARE-047
    private static @NotNull PendingChange runItem(final @NotNull StatusKind type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull JsonCodec mapper, final @NotNull Function<UUID, Optional<TestCaseDto>> testCases) {
        final @NotNull Optional<UUID> testCaseId = FileKind.RUN_ITEM.idIn(relativePath);
        final @NotNull Optional<TestCaseDto> testCase = testCaseId.flatMap(testCases);

        final @NotNull String name = testCase.map(TestCaseDto::getDescription).filter(description -> !description.isBlank())
                .orElseGet(() -> String.valueOf(relativePath.getFileName()));
        final @NotNull String testSet = testCase.map(TestCaseDto::getParent).map(AbstractNode::getName).orElse("");

        final @NotNull List<FieldChange> changes = switch (type) {
            case ADDED -> List.of(new FieldChange(parentName(relativePath), "",
                    RunItemFieldChanges.summary(read(mapper, afterJson, RunItem.class)), ChangeSubject.RUN_ITEM.getCreated()));
            case DELETED -> List.of(new FieldChange(parentName(relativePath),
                    RunItemFieldChanges.summary(read(mapper, beforeJson, RunItem.class)), "", ChangeSubject.RUN_ITEM.getRemoved()));
            case MODIFIED -> RunItemFieldChanges.compare(
                    read(mapper, beforeJson, RunItem.class), read(mapper, afterJson, RunItem.class));
        };

        return new PendingChange(ChangeSubject.RUN_ITEM, name, testSet, testCaseId.map(UUID::toString).orElse(""),
                relativePath, type, nothingCommitted(), changes);
    }

    private static @NotNull PendingChange marker(final @NotNull StatusKind type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull JsonCodec mapper) {
        final @NotNull String node = parentName(relativePath);
        final @NotNull String before = statusIn(mapper, beforeJson);
        final @NotNull String after = statusIn(mapper, afterJson);

        final @NotNull List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange(relativePath.getFileName().toString(), before, after, ChangeSubject.MARKER.changeFor(type)));

        if (type == StatusKind.MODIFIED && NodeType.TEST_RUN.isMarkerOf(relativePath.getFileName().toString())) {
            changes.addAll(TestRunMarkerFieldChanges.compareFacts(
                    read(mapper, beforeJson, TestRunMarker.class), read(mapper, afterJson, TestRunMarker.class)));
        }

        return new PendingChange(ChangeSubject.MARKER, node, "", "", relativePath, type, nothingCommitted(), changes);
    }

    // UC-SHARE-010, Rule-SHARE-047
    static @NotNull PendingChange unreadable(final @NotNull StatusKind type, final @NotNull Path relativePath) {
        return byName(subjectOf(relativePath), type, relativePath);
    }

    private static @NotNull PendingChange byName(final @NotNull ChangeSubject subject, final @NotNull StatusKind type, final @NotNull Path relativePath) {
        return new PendingChange(subject, relativePath.getFileName().toString(), "", "",
                relativePath, type, nothingCommitted(),
                List.of(new FieldChange(relativePath.toString(), "", "", subject.changeFor(type))));
    }

    private static @NotNull String statusIn(final @NotNull JsonCodec mapper, final @NotNull String json) {
        final @NotNull Object status = fieldsIn(mapper, json).get("status");
        return Objects.toString(status, "");
    }

    private static @NotNull String parentName(final @NotNull Path relativePath) {
        return Optional.ofNullable(relativePath.getParent()).map(Path::getFileName).map(Path::toString).orElse("");
    }

    private static <T> @NotNull T read(final @NotNull JsonCodec mapper, final @NotNull String json, final @NotNull Class<T> type) {
        if (json.isEmpty()) throw new IllegalStateException("Missing Git file revision");
        return mapper.readValue(json, type);
    }

    private static @NotNull TestCaseDto nothingCommitted() {
        return TestCaseDto.builder().build();
    }
}
