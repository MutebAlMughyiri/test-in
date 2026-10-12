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

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

// Rule-EDITOR-PANEL-292, Rule-INTERNAL-139
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Json {
    // Rule-EDITOR-PANEL-292, Rule-INTERNAL-139
    public static @NotNull FileType fileType() {
        return FileTypeManager.getInstance().getFileTypeByExtension("json");
    }

    // Rule-INTERNAL-139
    public static @NotNull Language language() {
        return fileType() instanceof LanguageFileType json ? json.getLanguage() : PlainTextLanguage.INSTANCE;
    }
}
