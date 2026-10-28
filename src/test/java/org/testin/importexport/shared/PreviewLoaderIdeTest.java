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

package org.testin.importexport.shared;

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.importexport.FileFormat;
import org.testin.model.TestCase;
import org.testin.ui.framework.TextValue;
import org.testin.util.Bundle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PreviewLoaderIdeTest extends AbstractTempRootIdeTest {
    private static final long LONGER_THAN_THE_PAUSE = 900;

    private final @NotNull AtomicReference<String> typed = new AtomicReference<>("");

    private final @NotNull List<String> messages = new CopyOnWriteArrayList<>();

    private final @NotNull List<String> read = new CopyOnWriteArrayList<>();

    private final @NotNull List<String> loaded = new CopyOnWriteArrayList<>();

    private final @NotNull TextValue box = new TextValue() {
        @Override
        public @NotNull String getText() {
            return Objects.requireNonNull(typed.get(), "nothing was typed");
        }

        @Override
        public void showEmptyWarning() {
        }
    };

    private static void waitLongerThanThePause() {
        final long until = System.currentTimeMillis() + LONGER_THAN_THE_PAUSE;
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
    }

    private @NotNull PreviewLoader loader(final boolean failing) {
        return new PreviewLoader(box, getProject(), messages::add, (_, data) -> loaded.addAll(data.keySet()), (file, _) -> readFile(file, failing));
    }

    private @NotNull Map<String, List<TestCase>> readFile(final @NotNull File file, final boolean failing) {
        read.add(file.getName());
        if (failing) throw new IllegalStateException("the file could not be parsed");

        return Map.of(file.getName(), List.of(TestCase.builder().description("log in with a valid user").build()));
    }

    private @NotNull String file(final @NotNull String name) {
        try {
            final @NotNull Path file = root.resolve(name);
            Files.writeString(file, """
                    Description
                    log in with a valid user
                    """);
            return file.toString();
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + name + ": " + ex.getMessage(), ex);
        }
    }

    private void type(final @NotNull PreviewLoader loader, final @NotNull String text) {
        typed.set(text);
        loader.pathChanged();
    }

    // UC-SHARE-005, Rule-SHARE-028
    public void testTheFileIsReadAsSoonAsTheBoxHoldsAPathTestinRecognizes() {
        final @NotNull PreviewLoader loader = loader(false);

        type(loader, file("notes.txt"));
        waitLongerThanThePause();
        assertEquals("a file Testin cannot import is not read", List.of(), read);

        type(loader, file("test cases" + FileFormat.CSV.getExtension()));
        Await.until("the CSV file was not read", () -> !loaded.isEmpty());
        assertEquals(List.of("test cases.csv"), loaded);
    }

    // UC-SHARE-005, Rule-SHARE-028
    public void testAPathToNoFileOrToAFolderIsNotRead() {
        final @NotNull PreviewLoader loader = loader(false);
        final @NotNull Path folder = root.resolve("sheets" + FileFormat.CSV.getExtension());
        try {
            Files.createDirectories(folder);
        } catch (final IOException ex) {
            throw new AssertionError("Could not make " + folder + ": " + ex.getMessage(), ex);
        }

        type(loader, root.resolve("missing" + FileFormat.CSV.getExtension()).toString());
        waitLongerThanThePause();
        type(loader, folder.toString());
        waitLongerThanThePause();

        assertEquals("a path to no file, or to a folder, was read", List.of(), read);
    }

    // UC-SHARE-005, Rule-SHARE-107
    public void testTheFileIsReadAfterTheLastKeystrokeAndTheFormSaysWhichWhileItReads() {
        final @NotNull PreviewLoader loader = loader(false);
        final @NotNull String first = file("first.csv");
        final @NotNull String second = file("second.csv");

        type(loader, first);
        type(loader, second);
        Await.until("the second file was not read", () -> !loaded.isEmpty());
        waitLongerThanThePause();

        assertEquals("only the path the typing stopped on is read", List.of("second.csv"), read);
        assertEquals(List.of(Bundle.message("import.reading", "second.csv"), ""), messages);
    }

    // UC-SHARE-005, Rule-SHARE-107
    public void testTheReadingLineGoesHoweverTheReadingStopped() {
        final @NotNull PreviewLoader loader = loader(true);

        type(loader, file("broken.csv"));
        Await.until("the reading line did not go after the reading failed", () -> messages.size() == 2);

        assertEquals(List.of(Bundle.message("import.reading", "broken.csv"), ""), messages);
        assertEquals(List.of(), loaded);
    }
}
