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

import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;

@Getter
public class UnifiedVirtualFile extends LightVirtualFile {
    private static final @NotNull String PROTOCOL = "testin";

    private final @NotNull AbstractNode dir;
    private final @NotNull EditorKind kind;

    public UnifiedVirtualFile(final @NotNull AbstractNode dir) {
        super(dir.getName());
        this.dir = dir;
        this.kind = EditorKind.of(dir);
        this.setFileType(kind.getFileType());
        putUserData(FileEditorProvider.KEY, new UnifiedEditorProvider());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @NotNull String getUrl() {
        return PROTOCOL + ":///" + dir.getPath().toAbsolutePath().toString().replace("\\", "/");
    }

    @Override
    public @NotNull String getPath() {
        return dir.getPath().toAbsolutePath().toString();
    }

    public @NotNull TestSetNode getTestSet() {
        return (TestSetNode) dir;
    }

    public @NotNull TestRunNode getTestRun() {
        return (TestRunNode) dir;
    }
}