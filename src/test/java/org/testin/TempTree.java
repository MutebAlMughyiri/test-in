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

package org.testin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TempTree {
    public static boolean delete(final @NotNull Path root) {
        if (!Files.exists(root)) return true;

        final @NotNull List<Path> deepestFirst;
        try (Stream<Path> walk = Files.walk(root)) {
            deepestFirst = walk.sorted(Comparator.reverseOrder()).toList();
        } catch (final IOException ex) {
            TestinLogger.warn("Could not clean up " + root + ": " + ex.getMessage());
            return false;
        }

        deepestFirst.forEach(TempTree::deleteOne);
        return !Files.exists(root);
    }

    private static void deleteOne(final @NotNull Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (final AccessDeniedException readOnly) {
            deleteReadOnly(path);
        } catch (final IOException ex) {
            TestinLogger.warn("Could not delete " + path + ": " + ex.getMessage());
        }
    }

    private static void deleteReadOnly(final @NotNull Path path) {
        try {
            Files.setAttribute(path, "dos:readonly", false);
            Files.deleteIfExists(path);
        } catch (final IOException | UnsupportedOperationException ex) {
            TestinLogger.warn("Could not delete the read-only " + path + ": " + ex.getMessage());
        }
    }
}
