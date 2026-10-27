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

package org.testin.git;

import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testin.git.LocalGit.git;
import static org.testin.git.LocalGit.mustGit;

public abstract class AbstractGitRemoteIdeTest extends AbstractTempRootIdeTest {
    protected static final @NotNull String MAIN = "main";

    protected static final @NotNull String LOGIN_TEST_CASE = "Test Cases/Login/login.testcase";

    protected Path remote;

    protected Path work;

    protected static void identify(final @NotNull Path repository, final @NotNull String name, final @NotNull String email) {
        mustGit(repository, "config", "user.name", name);
        mustGit(repository, "config", "user.email", email);
    }

    protected static void write(final @NotNull Path repository, final @NotNull String relativePath, final @NotNull String content) {
        try {
            final @NotNull Path file = repository.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + relativePath + ": " + ex.getMessage(), ex);
        }
    }

    protected static @NotNull String read(final @NotNull Path repository, final @NotNull String relativePath) {
        try {
            return Files.readString(repository.resolve(relativePath), StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + relativePath + ": " + ex.getMessage(), ex);
        }
    }

    protected static @NotNull String aTestCase(final @NotNull String description, final @NotNull String expected) {
        return "{\"description\":\"" + description + "\",\"expectedResult\":\"" + expected + "\"}";
    }

    protected static void commitAll(final @NotNull Path repository, final @NotNull String message) {
        mustGit(repository, "add", "-A");
        mustGit(repository, "commit", "-m", message);
    }

    protected static @NotNull String head(final @NotNull Path repository, final @NotNull String revision) {
        return mustGit(repository, "rev-parse", revision).trim();
    }

    protected static @NotNull Notification titled(final @NotNull List<Notification> said, final @NotNull String title) {
        Await.until("nothing was said under " + title, () -> said.stream().anyMatch(notification -> notification.getTitle().equals(title)));

        return said.stream().filter(notification -> notification.getTitle().equals(title)).findFirst().orElseThrow();
    }

    protected static @NotNull List<String> answers(final @NotNull Notification notification) {
        return notification.getActions().stream().map(AnAction::getTemplateText).toList();
    }

    @Override
    protected void setUp() {
        super.setUp();
        assertTrue("Git is not on the PATH, so a repository cannot be exercised", LocalGit.onThePath());

        remote = directory("remote.git");
        work = directory("work");

        mustGit(remote, "init", "--bare", "--initial-branch=" + MAIN);
        mustGit(work, "init", "--initial-branch=" + MAIN);
        identify(work, "Testin Test", "testin@example.invalid");
        mustGit(work, "remote", "add", "origin", remoteUrl());

        write(work, "first.testcase", "{}");
        commitAll(work, "first");
        mustGit(work, "push", "-u", "origin", MAIN);
    }

    protected @NotNull String remoteUrl() {
        return remote.toUri().toString();
    }

    protected @NotNull Path directory(final @NotNull String name) {
        try {
            return Files.createDirectories(root.resolve(name));
        } catch (final IOException ex) {
            throw new AssertionError("Could not make " + name + ": " + ex.getMessage(), ex);
        }
    }

    protected void bothChangeTheLoginTestCaseInAnotherField() {
        write(work, LOGIN_TEST_CASE, aTestCase("Log in", "The dashboard opens"));
        commitAll(work, "the test case");
        mustGit(work, "push", "origin", MAIN);

        final @NotNull Path colleague = colleague();
        write(colleague, LOGIN_TEST_CASE, aTestCase("Sign in", "The dashboard opens"));
        commitAll(colleague, "their wording");
        mustGit(colleague, "push", "origin", MAIN);

        write(work, LOGIN_TEST_CASE, aTestCase("Log in", "The account page opens"));
        commitAll(work, "my expected result");
    }

    protected void aPullStoppedOnTheLoginTestCase() {
        bothChangeTheLoginTestCaseInAnotherField();
        assertTrue("the pull did not stop on the test case", git(work, "pull", "--rebase", "origin", MAIN).isEmpty());
    }

    protected @NotNull Path colleague() {
        final @NotNull Path colleague = root.resolve("colleague");
        mustGit(root, "clone", remoteUrl(), colleague.toString());
        identify(colleague, "Colleague", "colleague@example.invalid");

        return colleague;
    }
}
