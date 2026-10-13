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

package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Once;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Trash {
    private static final @NotNull Key<Boolean> NO_BIN_SAID = Key.create("testin.trash.noBinSaid");

    // UC-INTERNAL-005, Rule-INTERNAL-036
    static boolean accepted(final @NotNull Project project, final @NotNull Path path) {
        if (!Files.exists(path) || ApplicationManager.getApplication().isUnitTestMode()) return false;

        if (movedToTheBin(path)) return true;

        sayThereIsNoBin(project);
        return false;
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    static boolean movedToTheBin(final @NotNull Path path) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            TestinLogger.debug("This desktop has no recycle bin; deleting " + path + " outright.");
            return false;
        }

        return movedTo(path, Desktop.getDesktop()::moveToTrash);
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    static boolean movedTo(final @NotNull Path path, final @NotNull Predicate<File> bin) {
        try {
            if (bin.test(path.toFile())) return true;

            TestinLogger.warn("The recycle bin refused " + path + ", deleting it instead.");
            return false;

        } catch (final Exception ex) {
            TestinLogger.warn("Could not move " + path + " to the recycle bin, deleting it instead: " + FailureText.of(ex));
            return false;
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    private static void sayThereIsNoBin(final @NotNull Project project) {
        if (!Once.claim(project, NO_BIN_SAID)) return;

        Services.getInstance(project, Notifier.class).info(project, Bundle.message("trash.none.title"), Bundle.message("trash.none.message"));
    }
}
