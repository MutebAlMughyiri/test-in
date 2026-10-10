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

package org.testin.editor.open;

import com.google.errorprone.annotations.Immutable;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;

import javax.swing.Icon;
import java.util.function.BiFunction;

@Getter
@Immutable
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EditorType extends FakeFileType {
    private final @NotNull String name;
    private final @NotNull String description;
    private final @NotNull Icon icon;
    private final @NotNull BiFunction<Project, UnifiedVirtualFile, TestinEditor> factory;

    @Override
    public boolean isMyFileType(final @NotNull VirtualFile file) {
        return false;
    }
}
