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

package org.testin.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRemotes;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Rule-INTERNAL-089, Rule-CODEGEN-082
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestinYml {
    public static final @NotNull String SCP_PREFIX = "git@";
    private static final @NotNull String[] FILE_NAMES = {"testin.yml", "testin.yaml"};
    private static final @NotNull Key<ParsedTestinYml> READ = Key.create("testin.yml");

    private static final @NotNull ParsedTestinYml NOTHING_SAID = new ParsedTestinYml(TestinProjectConfig.EMPTY, true);
    private static final @NotNull ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addHandler(new DeserializationProblemHandler() {
                @Override
                public boolean handleUnknownProperty(final @NotNull DeserializationContext context, final @NotNull JsonParser parser, final @NotNull JsonDeserializer<?> deserializer, final @NotNull Object beanOrClass, final @NotNull String key) {
                    TestinLogger.warn("Unknown key in testin.yml, ignored: " + key);

                    try {
                        parser.skipChildren();
                    } catch (final IOException ex) {
                        TestinLogger.warn("Could not skip past " + key + " in testin.yml: " + FailureText.of(ex));
                    }

                    return true;
                }
            });

    public static @NotNull @NlsSafe String fileName() {
        return FILE_NAMES[0];
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-104
    public static @NotNull @NlsSafe String bugRepoUrlKey() {
        return TestinProjectConfig.BUG_REPO_URL_KEY;
    }

    public static void reload(final @NotNull Project project) {
        project.putUserData(READ, load(project));
    }

    // UC-TREE-PANEL-001
    public static boolean isUnreadable(final @NotNull Project project) {
        return !parsedFor(project).readable();
    }

    public static @NotNull String projectName(final @NotNull Project project) {
        return config(project).projectName();
    }

    // Rule-CODEGEN-082
    public static boolean names(final @NotNull Project project, final @NotNull String projectName) {
        return !projectName.isEmpty() && projectName.equals(projectName(project));
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-003, Rule-SHARE-060
    public static @NotNull Optional<String> cloneAddress(final @NotNull Project project, final @NotNull String projectName) {
        final @NotNull TestinProjectConfig config = config(project);
        return names(project, projectName) && config.hasRepoUrl() ? Optional.of(config.repoUrl()) : Optional.empty();
    }

    // UC-TREE-PANEL-003
    public static boolean isRepoUrl(final @NotNull Project project, final @NotNull String address) {
        final @NotNull TestinProjectConfig config = config(project);
        return config.hasRepoUrl() && config.repoUrl().equals(TestinProjectConfig.withoutCredentials(address));
    }

    public static @NotNull String bugRepoUrl(final @NotNull Project project) {
        return config(project).bugRepoUrl();
    }

    // UC-TREE-PANEL-003, Rule-SHARE-062
    public static @NotNull String addressWithoutCredentials(final @NotNull String address) {
        return TestinProjectConfig.withoutCredentials(address);
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-071
    public static @NotNull String bugRepoUrlOnDisk(final @NotNull Project project) {
        return load(project).config().bugRepoUrl();
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-113
    public static @NotNull Map<String, String> lines(final @NotNull String projectName) {
        final @NotNull Map<String, String> lines = new LinkedHashMap<>();
        lines.put(TestinProjectConfig.PROJECT_KEY, projectName);
        return lines;
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-113, Rule-SHARE-004
    public static @NotNull Map<String, String> lines(final @NotNull String projectName, final @NotNull String remote) {
        final @NotNull Map<String, String> lines = lines(projectName);
        // Rule-TREE-PANEL-117: an address this file's reader would drop is not written as one
        final @NotNull String address = GitRemotes.isRepositoryUrl(remote) ? TestinProjectConfig.withoutCredentials(remote) : "";

        lines.put(TestinProjectConfig.LOCATION_KEY, (address.isEmpty() ? TestinLocation.LOCAL : TestinLocation.REMOTE).written());
        if (!address.isEmpty()) lines.put(TestinProjectConfig.REPO_URL_KEY, address);

        return lines;
    }

    // UC-TREE-PANEL-029
    public static @NotNull Optional<Path> savePath(final @NotNull Project project) {
        return file(project).or(() -> Optional.ofNullable(project.getBasePath()).map(base -> Path.of(base).resolve(FILE_NAMES[0])));
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-113
    public static @NotNull Map<String, String> writtenValues(final @NotNull Project project, final @NotNull Set<String> keys) {
        return valuesIn(currentText(project), keys);
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-114
    private static @NotNull String currentText(final @NotNull Project project) {
        final @NotNull Optional<Path> path = file(project);
        if (path.isEmpty()) return "";

        return openDocument(path.orElseThrow()).map(Document::getText).orElseGet(() -> textOf(path.orElseThrow()));
    }

    private static @NotNull Optional<Document> openDocument(final @NotNull Path path) {
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByNioFile(path))
                .map(file -> FileDocumentManager.getInstance().getCachedDocument(file));
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-112, Rule-TREE-PANEL-114
    public static boolean save(final @NotNull Project project, final @NotNull Map<String, String> owned) {
        final @NotNull Optional<Path> path = savePath(project);
        final @NotNull Optional<VirtualFile> folder = path.map(Path::getParent).map(LocalFileSystem.getInstance()::refreshAndFindFileByNioFile);
        if (folder.isEmpty()) {
            TestinLogger.warn("No folder to save testin.yml in for " + project.getName());
            return false;
        }

        final @NotNull String name = String.valueOf(path.orElseThrow().getFileName());
        final boolean saved = WriteCommandAction.writeCommandAction(project).withName(Bundle.message("yml.save.command", fileName()))
                .compute(() -> write(folder.orElseThrow(), name, owned));
        reload(project);
        return saved;
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-104
    public static boolean saveBugRepoUrl(final @NotNull Project project, final @NotNull String address) {
        return save(project, Map.of(TestinProjectConfig.BUG_REPO_URL_KEY, address));
    }

    private static boolean write(final @NotNull VirtualFile folder, final @NotNull String name, final @NotNull Map<String, String> owned) {
        try {
            final @NotNull Optional<VirtualFile> existing = Optional.ofNullable(folder.findChild(name));
            final @NotNull VirtualFile file = existing.isPresent() ? existing.orElseThrow() : folder.createChildData(TestinYml.class, name);
            final @NotNull Optional<Document> open = Optional.ofNullable(FileDocumentManager.getInstance().getCachedDocument(file));

            if (open.isPresent()) {
                final @NotNull Document document = open.orElseThrow();
                document.setText(withLines(document.getText(), owned));
                FileDocumentManager.getInstance().saveDocument(document);
            } else {
                VfsUtil.saveText(file, withLines(VfsUtilCore.loadText(file), owned));
            }

            TestinLogger.info("Saved " + file.getPath() + ": " + owned);
            return true;
        } catch (final IOException ex) {
            TestinLogger.warn("Could not save testin.yml in " + folder.getPath() + ": " + FailureText.of(ex));
            return false;
        }
    }

    // Rule-TREE-PANEL-114
    static @NotNull String withLines(final @NotNull String text, final @NotNull Map<String, String> owned) {
        final @NotNull String separator = text.contains("\r\n") ? "\r\n" : "\n";
        final @NotNull List<String> lines = new ArrayList<>(text.isEmpty() ? List.of() : Arrays.asList(text.replace("\r\n", "\n").split("\n", -1)));

        final boolean endedWithNewline = !lines.isEmpty() && lines.getLast().isEmpty();
        if (endedWithNewline) lines.removeLast();

        final @NotNull Set<String> placed = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            final int at = i;
            entryOf(lines.get(i), owned).ifPresent(entry -> {
                lines.set(at, line(entry.getKey(), entry.getValue()));
                placed.add(entry.getKey());
            });
        }

        final int before = lines.size();
        owned.forEach((key, value) -> {
            if (!placed.contains(key)) lines.add(line(key, value));
        });

        return String.join(separator, lines) + (endedWithNewline || lines.size() > before ? separator : "");
    }

    static @NotNull Map<String, String> valuesIn(final @NotNull String text, final @NotNull Set<String> keys) {
        if (text.isBlank()) return Map.of();

        try {
            final @NotNull Map<String, Object> read = Optional.ofNullable(YAML.readValue(text, new TypeReference<Map<String, Object>>() {
            })).orElse(Map.of());

            final @NotNull Map<String, String> values = new LinkedHashMap<>();
            keys.stream().filter(read::containsKey).forEach(key -> values.put(key, Objects.toString(read.get(key), "")));
            return values;
        } catch (final IOException ex) {
            TestinLogger.warn("Could not read the values in testin.yml: " + FailureText.of(ex));
            return Map.of();
        }
    }

    private static @NotNull Optional<Map.Entry<String, String>> entryOf(final @NotNull String line, final @NotNull Map<String, String> owned) {
        return owned.entrySet().stream().filter(entry -> line.startsWith(entry.getKey() + ":")).findFirst();
    }

    private static @NotNull String line(final @NotNull String key, final @NotNull String value) {
        return key + ": " + scalar(value);
    }

    private static @NotNull String scalar(final @NotNull String value) {
        final boolean plain = !value.isEmpty()
                && value.equals(value.strip())
                && "-?:,[]{}#&*!|>'\"%@`".indexOf(value.charAt(0)) < 0
                && !value.contains(": ") && !value.contains(" #") && !value.endsWith(":")
                && !Set.of("null", "~", "true", "false", "yes", "no", "on", "off").contains(value.toLowerCase(Locale.ROOT));

        return plain ? value : "'" + value.replace("'", "''") + "'";
    }

    private static @NotNull String textOf(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            TestinLogger.warn("Could not read " + file + ": " + FailureText.of(ex));
            return "";
        }
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-110
    public static void openInEditor(final @NotNull Project project) {
        file(project).flatMap(path -> Optional.ofNullable(LocalFileSystem.getInstance().findFileByNioFile(path)))
                .ifPresentOrElse(found -> FileEditorManager.getInstance(project).openFile(found, true),
                        () -> TestinLogger.warn("No testin.yml to open in " + project.getName()));
    }

    private static @NotNull TestinProjectConfig config(final @NotNull Project project) {
        return parsedFor(project).config();
    }

    private static @NotNull ParsedTestinYml parsedFor(final @NotNull Project project) {
        return Optional.ofNullable(project.getUserData(READ)).orElseGet(() -> {
            final @NotNull ParsedTestinYml read = load(project);
            project.putUserData(READ, read);
            return read;
        });
    }

    private static @NotNull ParsedTestinYml load(final @NotNull Project project) {
        final @NotNull ParsedTestinYml parsed = file(project)
                .map(TestinYml::read)
                .orElseGet(() -> {
                    TestinLogger.info("No testin.yml in " + project.getName() + "; Testin goes on without it");
                    return NOTHING_SAID;
                });

        if (!parsed.readable()) sayItCouldNotBeRead(project);

        return parsed;
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-119
    private static void sayItCouldNotBeRead(final @NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;

            Services.getInstance(project, Notifier.class).warn(project,
                    Bundle.message("config.broken", fileName()),
                    Bundle.message("config.broken.detail"));
        });
    }

    private static @NotNull ParsedTestinYml read(final @NotNull Path file) {
        try {
            return parsed(Files.readString(file), file.toString());
        } catch (final IOException ex) {
            TestinLogger.warn("Could not read " + file + ": " + FailureText.of(ex));
            return NOTHING_SAID;
        }
    }

    static @NotNull TestinProjectConfig parse(final @NotNull String yaml, final @NotNull String source) {
        return parsed(yaml, source).config();
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-119
    static @NotNull ParsedTestinYml parsed(final @NotNull String yaml, final @NotNull String source) {
        if (yaml.isBlank()) {
            TestinLogger.warn("Empty testin.yml: " + source);
            return NOTHING_SAID;
        }

        try {
            final @NotNull TestinProjectConfig config = YAML.readValue(yaml, TestinProjectConfig.class);
            TestinLogger.info("Read " + source + ": test project '" + config.projectName() + "'");
            return new ParsedTestinYml(config, true);

        } catch (final JsonProcessingException ex) {
            TestinLogger.warn("Malformed " + source + ", ignored: " + FailureText.of(ex));
            return new ParsedTestinYml(TestinProjectConfig.EMPTY, false);
        }
    }

    private static @NotNull Optional<Path> file(final @NotNull Project project) {
        final @NotNull Optional<String> basePath = Optional.ofNullable(project.getBasePath());
        if (basePath.isEmpty()) return Optional.empty();

        final @NotNull Path root = Path.of(basePath.orElseThrow());
        for (final String name : FILE_NAMES) {
            final @NotNull Path candidate = root.resolve(name);
            if (Files.isRegularFile(candidate)) return Optional.of(candidate);
        }

        return Optional.empty();
    }
}
