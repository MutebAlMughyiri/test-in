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

package org.testin.apimodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.notifications.Refused;
import org.testin.util.FailureText;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonModel {
    private static final @NotNull Set<String> NAMES_THE_FILE_USES = Set.of("Data", "Builder", "NoArgsConstructor", "AllArgsConstructor", "Accessors", "JsonProperty",
            "List", "String", "Integer", "Long", "Double", "Boolean", "Object", "BigInteger");

    private static final @NotNull JsonMapper READER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS, JsonReadFeature.ALLOW_SINGLE_QUOTES, JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    // UC-CODEGEN-022, Rule-CODEGEN-099
    public static @NotNull Optional<JsonNode> read(final @NotNull String pasted) {
        try {
            final @NotNull JsonNode json = READER.readTree(pasted);
            return json.isMissingNode() ? Optional.empty() : Optional.of(json);
        } catch (final JsonProcessingException notJson) {
            Logger.debug("The pasted text is not JSON: " + FailureText.of(notJson));
            return Optional.empty();
        }
    }

    // UC-CODEGEN-022, Rule-CODEGEN-099, Rule-CODEGEN-104
    public static @NotNull Optional<Refused> refusalOf(final @NotNull JsonNode json) {
        if (!json.isObject() && !json.isArray()) return Optional.of(Refused.NOT_A_JSON_OBJECT);
        if (json.isArray() && !elements(json).stream().filter(element -> !element.isNull()).allMatch(JsonNode::isObject))
            return Optional.of(Refused.NOT_A_JSON_OBJECT);

        final @NotNull JsonNode root = root(json);
        return root.isObject() && !root.isEmpty() ? Optional.empty() : Optional.of(Refused.NO_KEYS);
    }

    // UC-CODEGEN-022, Rule-CODEGEN-100, Rule-CODEGEN-104
    public static @NotNull ModelType of(final @NotNull String name, final @NotNull JsonNode json) {
        return typeOf(name, root(json), Set.of());
    }

    // Rule-CODEGEN-104
    private static @NotNull JsonNode root(final @NotNull JsonNode json) {
        return json.isArray() ? merged(elements(json)) : json;
    }

    // Rule-CODEGEN-100, Rule-CODEGEN-103
    private static @NotNull ModelType typeOf(final @NotNull String name, final @NotNull JsonNode object, final @NotNull Set<String> enclosing) {
        final @NotNull Set<String> around = new HashSet<>(enclosing);
        around.add(name);

        final @NotNull Set<String> fieldNames = new HashSet<>();
        final @NotNull List<ModelField> fields = new ArrayList<>();
        final @NotNull List<ModelType> nested = new ArrayList<>();
        object.properties().forEach(entry -> {
            final @NotNull String type = javaType(entry.getKey(), entry.getValue(), around, nested);
            fields.add(new ModelField(entry.getKey(), unique(NameSanitizer.modelFieldName(entry.getKey()), fieldNames), type));
        });

        return new ModelType(name, List.copyOf(fields), List.copyOf(nested));
    }

    // Rule-CODEGEN-102
    private static @NotNull String javaType(final @NotNull String key, final @NotNull JsonNode value, final @NotNull Set<String> around, final @NotNull List<ModelType> nested) {
        if (value.isTextual()) return "String";
        if (value.isBoolean()) return "Boolean";
        if (value.isInt()) return "Integer";
        if (value.isLong()) return "Long";
        if (value.isBigInteger()) return "BigInteger";
        if (value.isNumber()) return "Double";
        if (value.isObject()) return nestedType(NameSanitizer.modelTypeName(key), value, around, nested);
        if (value.isArray()) return "List<" + elementType(key, value, around, nested) + ">";
        return "Object";
    }

    // Rule-CODEGEN-102
    private static @NotNull String elementType(final @NotNull String key, final @NotNull JsonNode list, final @NotNull Set<String> around, final @NotNull List<ModelType> nested) {
        final @NotNull JsonNode element = merged(elements(list));
        if (element.isObject()) return nestedType(NameSanitizer.modelTypeName(key) + "Item", element, around, nested);

        return javaType(key, element, around, nested);
    }

    // Rule-CODEGEN-103
    private static @NotNull String nestedType(final @NotNull String wanted, final @NotNull JsonNode object, final @NotNull Set<String> around, final @NotNull List<ModelType> nested) {
        final @NotNull Set<String> taken = new HashSet<>(around);
        taken.addAll(NAMES_THE_FILE_USES);
        nested.forEach(type -> taken.add(type.name()));

        final @NotNull String name = unique(NAMES_THE_FILE_USES.contains(wanted) ? wanted + "Item" : wanted, taken);
        nested.add(typeOf(name, object, around));
        return name;
    }

    // Rule-CODEGEN-103
    private static @NotNull String unique(final @NotNull String wanted, final @NotNull Set<String> taken) {
        String name = wanted;
        for (int next = 2; taken.contains(name); next++) {
            name = wanted + next;
        }
        taken.add(name);
        return name;
    }

    // Rule-CODEGEN-102
    private static @NotNull JsonNode merged(final @NotNull List<JsonNode> values) {
        if (values.stream().anyMatch(JsonNode::isMissingNode)) return MissingNode.getInstance();

        final @NotNull List<JsonNode> known = values.stream().filter(value -> !value.isNull()).toList();
        if (known.isEmpty()) return NullNode.getInstance();
        if (known.stream().allMatch(JsonNode::isObject)) return mergedObjects(known);
        if (known.stream().allMatch(JsonNode::isArray)) return mergedLists(known);
        if (known.stream().allMatch(JsonNode::isNumber)) return widest(known);
        if (known.stream().allMatch(JsonNode::isTextual) || known.stream().allMatch(JsonNode::isBoolean))
            return known.getFirst();

        return MissingNode.getInstance();
    }

    // Rule-CODEGEN-102
    private static @NotNull ObjectNode mergedObjects(final @NotNull List<JsonNode> objects) {
        final @NotNull Map<String, List<JsonNode>> byKey = new LinkedHashMap<>();
        objects.forEach(object -> object.properties().forEach(entry -> byKey.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>()).add(entry.getValue())));

        final @NotNull ObjectNode merged = JsonNodeFactory.instance.objectNode();
        byKey.forEach((key, all) -> merged.set(key, merged(all)));
        return merged;
    }

    // Rule-CODEGEN-102
    private static @NotNull ArrayNode mergedLists(final @NotNull List<JsonNode> lists) {
        final @NotNull ArrayNode merged = JsonNodeFactory.instance.arrayNode();
        lists.forEach(list -> merged.addAll(elements(list)));
        return merged;
    }

    // Rule-CODEGEN-102
    private static @NotNull JsonNode widest(final @NotNull List<JsonNode> numbers) {
        if (numbers.stream().anyMatch(JsonNode::isFloatingPointNumber)) return DoubleNode.valueOf(0);
        if (numbers.stream().anyMatch(JsonNode::isBigInteger))
            return numbers.stream().filter(JsonNode::isBigInteger).findFirst().orElseThrow();
        if (numbers.stream().anyMatch(JsonNode::isLong))
            return numbers.stream().filter(JsonNode::isLong).findFirst().orElseThrow();

        return numbers.getFirst();
    }

    private static @NotNull List<JsonNode> elements(final @NotNull JsonNode list) {
        return StreamSupport.stream(list.spliterator(), false).toList();
    }
}
