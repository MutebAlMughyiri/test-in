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

package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class ExportJson {
    // UC-SHARE-001, Rule-SHARE-010
    public void exportToFile(final @NotNull Project project, final @NotNull File destFile, final @NotNull Map<String, List<TestCase>> sheetsData) {
        try {
            FileUtil.createParentDirs(destFile);

            Files.write(destFile.toPath(), Services.getInstance(project, JsonCodec.class).writeValueAsBytes(sheetsData));
        } catch (final IOException ex) {
            TestinLogger.error("export failed: " + destFile + " - " + FailureText.of(ex));
            throw new RuntimeException(ex);
        }
    }
}
