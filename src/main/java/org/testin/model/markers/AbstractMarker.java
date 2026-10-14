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

package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DateFormats;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
// Rule-INTERNAL-135
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ToString
public abstract class AbstractMarker implements Marker {
    // Rule-INTERNAL-090, Rule-TREE-PANEL-051, Rule-INTERNAL-083
    @NonNull
    private String uuid = "";

    private int order = Marker.NOT_ORDERED;

    @NonNull
    private String createdBy = "";

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateFormats.DATE_FORMAT_PATTERN, locale = DateFormats.DATE_FORMAT_LOCALE)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    private String updatedBy = "";

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateFormats.DATE_FORMAT_PATTERN, locale = DateFormats.DATE_FORMAT_LOCALE)
    private ZonedDateTime updatedAt = DateFormats.NOT_EXECUTED;

    @Override
    public @NotNull String getUpdatedBy() {
        return updatedBy.isBlank() ? createdBy : updatedBy;
    }

    @Override
    public @NotNull ZonedDateTime getUpdatedAt() {
        return DateFormats.isNotExecuted(updatedAt) ? createdAt : updatedAt;
    }

}
