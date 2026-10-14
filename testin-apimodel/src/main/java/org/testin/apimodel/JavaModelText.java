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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaModelText {
    private static final char OPEN = '{';

    private static final char CLOSE = '}';

    private static final @NotNull String INDENT = "    ";

    private static final @NotNull String CONTINUATION = INDENT + INDENT;

    private static final @NotNull String JAVA = "java.";

    private static final @NotNull String JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";

    private static final @NotNull List<String> LOMBOK_ON_EVERY_CLASS = List.of("lombok.Data", "lombok.NoArgsConstructor");

    private static final @NotNull List<String> LOMBOK_ON_A_CLASS_WITH_FIELDS = List.of("lombok.Builder", "lombok.AllArgsConstructor", "lombok.experimental.Accessors");

    // UC-CODEGEN-022, Rule-CODEGEN-100, Rule-CODEGEN-101
    public static @NotNull String requestClass(final @NotNull String packageName, final @NotNull ModelType root) {
        final @NotNull Set<String> imports = importsOf(root);
        imports.addAll(LOMBOK_ON_EVERY_CLASS);
        if (root.withAllNested().anyMatch(type -> !type.fields().isEmpty()))
            imports.addAll(LOMBOK_ON_A_CLASS_WITH_FIELDS);

        final @NotNull List<String> lines = new ArrayList<>(head(packageName, imports));
        lines.addAll(classLines(root, "", "public class "));
        return String.join("\n", lines) + "\n";
    }

    // UC-CODEGEN-022, Rule-CODEGEN-100, Rule-CODEGEN-101
    public static @NotNull String responseRecord(final @NotNull String packageName, final @NotNull ModelType root) {
        final @NotNull List<String> lines = new ArrayList<>(head(packageName, importsOf(root)));
        lines.addAll(recordLines(root, ""));
        return String.join("\n", lines) + "\n";
    }

    // Rule-CODEGEN-101
    private static @NotNull Set<String> importsOf(final @NotNull ModelType root) {
        final @NotNull Set<String> imports = new TreeSet<>();
        final @NotNull List<ModelField> fields = root.withAllNested().flatMap(type -> type.fields().stream()).toList();
        if (fields.stream().anyMatch(ModelField::isRenamed)) imports.add(JSON_PROPERTY);
        if (fields.stream().anyMatch(field -> field.type().contains("List<"))) imports.add("java.util.List");
        if (fields.stream().anyMatch(field -> field.type().contains("BigInteger"))) imports.add("java.math.BigInteger");
        return imports;
    }

    // Rule-CODEGEN-101
    private static @NotNull List<String> head(final @NotNull String packageName, final @NotNull Set<String> imports) {
        final @NotNull List<String> lines = new ArrayList<>();
        if (!packageName.isEmpty()) {
            lines.add("package " + packageName + ";");
            lines.add("");
        }
        final @NotNull List<String> others = imports.stream().filter(name -> !name.startsWith(JAVA)).toList();
        final @NotNull List<String> java = imports.stream().filter(name -> name.startsWith(JAVA)).toList();
        Stream.of(others, java).filter(group -> !group.isEmpty()).forEach(group -> {
            group.forEach(name -> lines.add("import " + name + ";"));
            lines.add("");
        });
        return lines;
    }

    // Rule-CODEGEN-100, Rule-CODEGEN-101
    private static @NotNull List<String> classLines(final @NotNull ModelType type, final @NotNull String indent, final @NotNull String declaration) {
        final @NotNull List<String> lines = new ArrayList<>();
        lines.add(indent + "@Data");
        if (!type.fields().isEmpty()) lines.add(indent + "@Builder");
        lines.add(indent + "@NoArgsConstructor");
        if (!type.fields().isEmpty()) {
            lines.add(indent + "@AllArgsConstructor");
            lines.add(indent + "@Accessors(chain = true)");
        }
        lines.add(indent + declaration + type.name() + " " + OPEN);

        final @NotNull String inner = indent + INDENT;
        lines.addAll(fieldLines(type.fields(), inner, "private ", ";", ";"));
        for (final ModelType nested : type.nested()) {
            lines.add("");
            lines.addAll(classLines(nested, inner, "public static class "));
        }

        lines.add(indent + CLOSE);
        return lines;
    }

    // Rule-CODEGEN-100, Rule-CODEGEN-101
    private static @NotNull List<String> recordLines(final @NotNull ModelType type, final @NotNull String indent) {
        final @NotNull List<String> lines = new ArrayList<>();
        if (type.fields().isEmpty()) {
            lines.add(indent + "public record " + type.name() + "() " + OPEN);
        } else {
            lines.add(indent + "public record " + type.name() + "(");
            lines.addAll(fieldLines(type.fields(), indent + CONTINUATION, "", ",", ") " + OPEN));
        }

        for (final ModelType nested : type.nested()) {
            lines.add("");
            lines.addAll(recordLines(nested, indent + INDENT));
        }

        lines.add(indent + CLOSE);
        return lines;
    }

    // Rule-CODEGEN-101
    private static @NotNull List<String> fieldLines(final @NotNull List<ModelField> fields, final @NotNull String indent, final @NotNull String modifier, final @NotNull String end, final @NotNull String lastEnd) {
        final @NotNull List<String> lines = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            final @NotNull ModelField field = fields.get(i);
            if (i > 0) lines.add("");
            if (field.isRenamed()) lines.add(indent + jsonProperty(field));
            lines.add(indent + modifier + field.type() + " " + field.name() + (i == fields.size() - 1 ? lastEnd : end));
        }
        return lines;
    }

    // Rule-CODEGEN-101
    private static @NotNull String jsonProperty(final @NotNull ModelField field) {
        return "@JsonProperty(\"" + field.key().replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }
}
