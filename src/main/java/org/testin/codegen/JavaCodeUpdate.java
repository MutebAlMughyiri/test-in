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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.event.CodeUpdate;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Once;

import java.util.List;

public enum JavaCodeUpdate implements CodeUpdate {
    INSTANCE;

    private static final @NotNull Key<Boolean> INDEXING_SAID = Key.create("testin.codegen.indexingSaid");

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-006, Rule-CODEGEN-082
    private static boolean cannotGenerate(final @NotNull Project p, final @NotNull String description) {
        if (!CodeOn.isOnOrHinted(p)) return true;
        if (!DumbService.isDumb(p)) return false;

        if (Once.claim(p, INDEXING_SAID)) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.WHILE_INDEXING, description);

            DumbService.getInstance(p).runWhenSmart(() -> p.putUserData(INDEXING_SAID, null));
        }

        TestinLogger.info("Skipped " + description + ": the IDE is indexing");
        return true;
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005
    @Override
    public <T> void execute(final @NotNull GenType<T> type, final @NotNull Project p, final @NotNull T payload) {
        if (cannotGenerate(p, type.description())) return;

        CodeGenerators.find(type).execute(p, payload);
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-EDITOR-PANEL-046
    @Override
    public <T> void executeAll(final @NotNull GenType<T> type, final @NotNull Project p, final @NotNull List<? extends T> items, final @NotNull String undoGroup) {
        if (cannotGenerate(p, type.description()) || items.isEmpty()) return;

        WriteCommandAction.runWriteCommandAction(p, type.description(), undoGroup,
                () -> CodeGenerators.find(type).executeAll(p, items));
    }
}
