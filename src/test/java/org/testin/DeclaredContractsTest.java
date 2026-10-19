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

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.testng.Assert.assertTrue;

public class DeclaredContractsTest {

    private static final @NotNull List<Path> ROOTS = List.of(Path.of("src", "main", "java"), Path.of("src", "test", "java"), Path.of("testin-java", "src", "main", "java"), Path.of("testin-java", "src", "test", "java"), Path.of("testin-testng", "src", "main", "java"), Path.of("testin-apimodel", "src", "main", "java"), Path.of("testin-apimodel", "src", "test", "java"));

    private static final @NotNull Pattern THROWS = Pattern.compile("^\\s+(?!return\\b)[^=;]*?\\b(\\w+)\\s*\\(.*\\)\\s+throws\\s+[\\w., ]+\\s*[{;]\\s*$");

    private static final @NotNull Pattern NULLABLE_RETURN = Pattern.compile("^\\s+(?:(?:public|protected|private|static|final|default|abstract|synchronized)\\s+)*(?:<[^>]+>\\s+)?@Nullable\\s+[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(");

    private static final @NotNull Map<String, String> MAY_THROW = Map.ofEntries(
            entry("TestCaseTransferHandler.getTransferData", "AWT's Transferable contract: a flavor the transferable does not carry throws"),
            entry("TreeTransferable.getTransferData", "AWT's Transferable contract: a flavor the transferable does not carry throws"),
            entry("SettingsConfigurable.apply", "Configurable.apply declares ConfigurationException, and the settings dialog shows its message"),
            entry("AgentPromptConfigurable.apply", "Configurable.apply declares ConfigurationException, and the settings dialog shows its message")
    );

    private static final @NotNull Map<String, String> MAY_RETURN_NULL = Map.ofEntries(
            entry("TestCaseTransferHandler.createTransferable", "TransferHandler's contract: null is nothing to drag"),
            entry("TreeTransferHandler.createTransferable", "TransferHandler's contract: null is nothing to drag"),
            entry("TreeDropHandler.handleDrop", "FileDropHandler is a Kotlin suspend function, whose Java face answers an Object that may be null"),
            entry("SelectionTable.getToolTipText", "JComponent's contract: null is no tooltip"),
            entry("TreePanelStructure.getParentElement", "AbstractTreeStructure's contract: null is the root"),
            entry("TestinFileWatcher.prepareChange", "AsyncFileListener's contract: null is nothing to apply"),
            entry("ViewOnScreen.getActiveToolWindowId", "test fixture: ToolWindowManager's contract, null is no tool window active"),
            entry("ViewOnScreen.getLastActiveToolWindowId", "test fixture: ToolWindowManager's contract, null is that none has been active"),
            entry("ViewOnScreen.getToolWindow", "test fixture: ToolWindowManager's contract, null is no such tool window"),
            entry("ViewOnScreen.getToolWindowBalloon", "test fixture: ToolWindowManager's contract, null is no balloon shown")
    );

    private static @NotNull Set<String> declared(final @NotNull Pattern declaration) {
        final @NotNull Set<String> found = new TreeSet<>();

        for (final Path root : ROOTS) {
            if (!Files.isDirectory(root)) continue;

            try (Stream<Path> files = Files.walk(root)) {
                files.filter(file -> file.toString().endsWith(".java")).forEach(file -> collect(file, declaration, found));
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        return found;
    }

    private static void collect(final @NotNull Path file, final @NotNull Pattern declaration, final @NotNull Set<String> found) {
        final @NotNull String owner = file.getFileName().toString().replace(".java", "");

        try {
            for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                final @NotNull Matcher matcher = declaration.matcher(line);
                if (matcher.find()) found.add(owner + "." + matcher.group(1));
            }
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void assertOnlyTheListed(final @NotNull Set<String> found, final @NotNull Set<String> listed, final @NotNull String what, final @NotNull String rule) {
        final @NotNull Set<String> unlisted = new TreeSet<>(found);
        unlisted.removeAll(listed);

        final @NotNull Set<String> stale = new TreeSet<>(listed);
        stale.removeAll(found);

        assertTrue(unlisted.isEmpty(), unlisted + " " + what + ", and " + rule
                + " If a platform or interface contract requires it, add it to the list here with that contract as its reason.");
        assertTrue(stale.isEmpty(), stale + " are on the list and no longer " + what + ". Take them off, so the list stays the whole truth.");
    }

    @Test
    public void onlyAContractDeclaresThrows() {
        assertOnlyTheListed(declared(THROWS), MAY_THROW.keySet(), "declare throws", "a method handles its own failures rather than deferring them to a caller that knows less (CLAUDE.md).");
    }

    @Test
    public void onlyAContractReturnsNull() {
        assertOnlyTheListed(declared(NULLABLE_RETURN), MAY_RETURN_NULL.keySet(), "return @Nullable", "absence is an empty value, not a null (Decision-003).");
    }
}
