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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.services.Services;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.APP)
@NoArgsConstructor
public final class DeletedNodes {
    private final @NotNull Path staging = Path.of(PathManager.getSystemPath(), "testin", "deleted");

    private final @NotNull AtomicBoolean swept = new AtomicBoolean();

    private final @NotNull OwnWrites ownWrites = Services.getInstance(OwnWrites.class);

    // UC-INTERNAL-005, Rule-INTERNAL-044
    public void sweep() {
        if (!swept.compareAndSet(false, true)) return;
        if (!Files.isDirectory(staging)) return;

        if (FileUtil.delete(staging.toFile()))
            TestinLogger.info("Cleared the copies kept for undo by the previous session.");
        else TestinLogger.warn("Could not clear " + staging + "; copies from the previous session are still there.");
    }

    // UC-INTERNAL-005, Rule-INTERNAL-037, Rule-INTERNAL-039, Rule-INTERNAL-041
    public @NotNull Optional<Path> keep(final @NotNull Path node) {
        if (!Files.exists(node)) return Optional.empty();

        final @NotNull Path kept = staging.resolve(UUID.randomUUID().toString()).resolve(node.getFileName().toString());

        try {
            Files.createDirectories(kept.getParent());

            if (Files.isDirectory(node)) FileUtil.copyDir(node.toFile(), kept.toFile());
            else Files.copy(node, kept, StandardCopyOption.REPLACE_EXISTING);

            return Optional.of(kept);

        } catch (final IOException ex) {
            TestinLogger.warn("Could not keep " + node + " aside, so removing it will not be undoable: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-042, Rule-INTERNAL-113
    public boolean putBack(final @NotNull Project p, final @NotNull Path kept, final @NotNull Path original) {
        if (Files.exists(original)) {
            TestinLogger.warn("Not restoring " + original + ": something is there already.");
            return false;
        }

        try {
            ownWrites.record(p, original);
            Files.createDirectories(original.getParent());

            if (Files.isDirectory(kept)) FileUtil.copyDir(kept.toFile(), original.toFile());
            else Files.copy(kept, original, StandardCopyOption.REPLACE_EXISTING);

            return true;

        } catch (final IOException ex) {
            TestinLogger.error("Could not restore " + original + " from " + kept + ": " + FailureText.of(ex));
            return false;
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-043
    public void forget(final @NotNull Path kept) {
        final @NotNull Path keptIn = Objects.requireNonNull(kept.getParent(), kept.toString());
        if (!FileUtil.delete(keptIn.toFile()))
            TestinLogger.warn("Left a kept copy of a removed node behind at " + keptIn);
    }
}
