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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ExtensionTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.event.Moved;
import org.testin.codegen.event.Renamed;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;

import java.util.ArrayList;
import java.util.List;

public final class CodegenRecorder implements CodeGenerators {
    private final @NotNull CodeGenerators real;

    private final @NotNull List<String> asked = new ArrayList<>();

    private CodegenRecorder(final @NotNull CodeGenerators real) {
        this.real = real;
    }

    public static @NotNull CodegenRecorder installed(final @NotNull Disposable until) {
        final @NotNull CodegenRecorder recorder = new CodegenRecorder(EP.getExtensionList().getFirst());
        ExtensionTestUtil.maskExtensions(EP, List.of(recorder), until);
        return recorder;
    }

    public static @NotNull String step(final @NotNull GenType<?> type, final @NotNull String name) {
        return type.description() + ": " + name;
    }

    private static @NotNull String nameOf(final @NotNull Object payload) {
        return switch (payload) {
            case final TestCaseDto tc -> tc.getDescription();
            case final AbstractNode dir -> dir.getName();
            case final Moved moved -> moved.dir().getName();
            case final Renamed renamed -> renamed.dir().getName();
            default -> payload.toString();
        };
    }

    public @NotNull List<String> asked() {
        return List.copyOf(asked);
    }

    @Override
    public <T> @NotNull GenAction<T> actionFor(final @NotNull GenType<T> type) {
        final @NotNull GenAction<T> action = real.actionFor(type);

        return new GenAction<>() {
            @Override
            public void execute(final @NotNull Project p, final @NotNull T payload) {
                asked.add(step(type, nameOf(payload)));
                action.execute(p, payload);
            }

            @Override
            public void executeAll(final @NotNull Project p, final @NotNull List<? extends T> items) {
                items.forEach(item -> asked.add(step(type, nameOf(item))));
                action.executeAll(p, items);
            }

            @Override
            public boolean generates() {
                return action.generates();
            }
        };
    }
}
