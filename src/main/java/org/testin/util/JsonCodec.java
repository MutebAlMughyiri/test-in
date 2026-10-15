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

package org.testin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;

import java.io.IOException;
import java.io.UncheckedIOException;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonCodec {
    // UC-INTERNAL-002, Rule-INTERNAL-070
    private final @NotNull ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    private static @NotNull UncheckedIOException unreadable(final @NotNull String type, final @NotNull IOException ex) {
        return new UncheckedIOException("Not a readable " + type + ": " + FailureText.of(ex), ex);
    }

    public @NotNull <T> T readValue(final byte @NotNull [] bytes, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(bytes, valueType);
        } catch (final IOException ex) {
            throw unreadable(valueType.getSimpleName(), ex);
        }
    }

    public @NotNull <T> T readValue(final byte @NotNull [] bytes, final @NotNull TypeReference<T> valueType) {
        try {
            return mapper.readValue(bytes, valueType);
        } catch (final IOException ex) {
            throw unreadable(valueType.getType().getTypeName(), ex);
        }
    }

    public @NotNull <T> T readValue(final @NotNull String content, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(content, valueType);
        } catch (final JsonProcessingException ex) {
            throw unreadable(valueType.getSimpleName(), ex);
        }
    }

    public @NotNull <T> T readValue(final @NotNull String content, final @NotNull TypeReference<T> valueType) {
        try {
            return mapper.readValue(content, valueType);
        } catch (final JsonProcessingException ex) {
            throw unreadable(valueType.getType().getTypeName(), ex);
        }
    }

    // Rule-PRODUCT-003
    public byte @NotNull [] writeValueAsBytes(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), ex);
        }
    }

    public @NotNull String writeValueAsString(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), ex);
        }
    }

    public @NotNull ObjectNode readTree(final @NotNull String content) {
        try {
            final @NotNull JsonNode node = mapper.readTree(content);
            return node instanceof ObjectNode object ? object : mapper.createObjectNode();
        } catch (final JsonProcessingException ex) {
            TestinLogger.debug("JsonCodec.readTree() could not parse the content: " + FailureText.of(ex));
            return mapper.createObjectNode();
        }
    }
}
