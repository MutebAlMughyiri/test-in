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

package org.testin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
// Rule-INTERNAL-135
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ToString
public final class TestCase {
    @NonNull
    @Builder.Default
    private volatile String order = "";

    // Rule-INTERNAL-012
    @JsonIgnore
    @NonNull
    @Builder.Default
    private volatile UUID id = UUID.randomUUID();

    @NonNull
    @Builder.Default
    private volatile String description = "";

    @NonNull
    @Builder.Default
    private volatile String expectedResult = "";

    @NonNull
    @Builder.Default
    private volatile TestCaseStatus status = TestCaseStatus.PENDING;

    @NonNull
    @Builder.Default
    private volatile List<String> steps = new ArrayList<>();

    @NonNull
    @Builder.Default
    private volatile Priority priority = Priority.DEFAULT;

    @NonNull
    @Builder.Default
    @JsonIgnore
    private volatile TestSetNode parent = new TestSetNode();

    @NonNull
    @Builder.Default
    private volatile String reference = "";

    @NonNull
    @Builder.Default
    private volatile List<String> groups = new ArrayList<>();

    @NonNull
    @Builder.Default
    private volatile String createdBy = "";

    @NonNull
    @Builder.Default
    private volatile String updatedBy = "";

    @NonNull
    @Builder.Default
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateFormats.DATE_FORMAT_PATTERN, locale = DateFormats.DATE_FORMAT_LOCALE)
    private volatile ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateFormats.DATE_FORMAT_PATTERN, locale = DateFormats.DATE_FORMAT_LOCALE)
    private volatile ZonedDateTime updatedAt = DateFormats.NOT_EXECUTED;

    @NonNull
    @Builder.Default
    private volatile String module = "";

    @NonNull
    @Builder.Default
    private volatile String testData = "";

    @NonNull
    @Builder.Default
    private volatile String preConditions = "";

    // UC-INTERNAL-002, Rule-INTERNAL-012
    public static @NotNull TestCase read(final @NotNull JsonCodec mapper, final @NotNull String json, final @NotNull UUID testCaseId) {
        return mapper.readValue(json, TestCase.class).setId(testCaseId);
    }

    public static @NotNull TestCase deleted(final @NotNull UUID testCaseId) {
        return TestCase.builder()
                .id(testCaseId)
                .description(Bundle.message("testcase.deleted", testCaseId))
                .build();
    }

    public void stampCreated(final @NotNull String tester) {
        createdBy = tester;
        createdAt = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);

        updatedBy = "";
        updatedAt = DateFormats.NOT_EXECUTED;
    }

    public void touch(final @NotNull String tester) {
        updatedBy = tester;
        updatedAt = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
    }

    public void takeAuditOf(final @NotNull TestCase other) {
        createdBy = other.createdBy;
        createdAt = other.createdAt;
        updatedBy = other.updatedBy;
        updatedAt = other.updatedAt;
    }

    // Rule-INTERNAL-117
    public @NotNull TestCaseBuilder edit() {
        return toBuilder().steps(new ArrayList<>(steps)).groups(new ArrayList<>(groups));
    }

    // Rule-INTERNAL-117, Rule-EDITOR-PANEL-238
    public @NotNull TestCase copy() {
        return edit().build();
    }

    // Rule-INTERNAL-117
    public @NotNull TestCase takeValuesOf(final @NotNull TestCase edited) {
        order = edited.order;
        description = edited.description;
        expectedResult = edited.expectedResult;
        status = edited.status;
        steps = new ArrayList<>(edited.steps);
        priority = edited.priority;
        parent = edited.parent;
        reference = edited.reference;
        groups = new ArrayList<>(edited.groups);
        module = edited.module;
        testData = edited.testData;
        preConditions = edited.preConditions;
        takeAuditOf(edited);
        return this;
    }
}