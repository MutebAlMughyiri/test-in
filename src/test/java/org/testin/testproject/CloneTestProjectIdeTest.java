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

package org.testin.testproject;

import com.intellij.openapi.util.Disposer;
import com.sun.net.httpserver.HttpServer;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.explorer.TreePanel;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.testin.git.LocalGit.mustGit;

public class CloneTestProjectIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull Path elsewhere = Path.of("");

    @Override
    protected void setUp() {
        super.setUp();
        try {
            elsewhere = Files.createTempDirectory("testin-clone-source");
        } catch (final IOException ex) {
            throw new AssertionError("could not make a folder for the repository to clone: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void tearDown() {
        FileUtil.delete(elsewhere.toFile());
        super.tearDown();
    }

    private @NotNull String aRepositoryHoldingATestProject() {
        final @NotNull Path repository = aTestProjectAt(elsewhere.resolve("checkout-test-cases"));
        aTestCaseIn(theTestCasesOf(repository));
        mustGit(repository, "init", "--initial-branch=main");
        mustGit(repository, "config", "user.name", "Colleague");
        mustGit(repository, "config", "user.email", "colleague@example.invalid");
        mustGit(repository, "add", "-A");
        mustGit(repository, "commit", "-m", "the test project");
        return repository.toUri().toString();
    }

    private @NotNull String servedOverHttpWithAPassword(final @NotNull String repository) {
        mustGit(elsewhere, "clone", "--bare", repository, elsewhere.resolve("served.git").toString());
        mustGit(elsewhere.resolve("served.git"), "update-server-info");
        try {
            final @NotNull HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> {
                final @NotNull Path file = elsewhere.resolve(exchange.getRequestURI().getPath().substring(1));
                final byte @NotNull [] body = Files.isRegularFile(file) ? Files.readAllBytes(file) : new byte[0];
                exchange.sendResponseHeaders(body.length == 0 ? 404 : 200, body.length == 0 ? -1 : body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            Disposer.register(getTestRootDisposable(), () -> server.stop(0));
            return "http://tester:secret@127.0.0.1:" + server.getAddress().getPort() + "/served.git";
        } catch (final IOException ex) {
            throw new AssertionError("could not serve the repository over HTTP: " + ex.getMessage(), ex);
        }
    }

    private @NotNull TreePanel aTreePanel() {
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
        return panel;
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-106, Rule-TREE-PANEL-107
    public void testACloneLandsInTheTestinFolderIsReadAndBecomesTheChosenTestProject() {
        readEverything();
        final @NotNull Said told = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);

        new CloneTestProject(getProject(), aRepositoryHoldingATestProject(), "CHECKOUT", aTreePanel()).execute();

        Await.until("the clone never said it was done", () -> told.shown().contains(Done.CLONED.getOutcome()));
        assertTrue("the clone did not land in the Testin folder", Files.isDirectory(root.resolve("CHECKOUT").resolve(".git")));
        assertTrue("the cloned test project was not read", nodes().isTaken(root.resolve("CHECKOUT"), Optional.empty()));
        assertEquals("the cloned test project is not the chosen one", "CHECKOUT", bound.name());
    }

    // UC-TREE-PANEL-003, Rule-SHARE-062
    public void testAPasswordInTheAddressIsNotKeptInTheClonesRemote() {
        readEverything();
        final @NotNull Said told = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull String address = servedOverHttpWithAPassword(aRepositoryHoldingATestProject());

        new CloneTestProject(getProject(), address, "CHECKOUT", aTreePanel()).execute();

        Await.until("the clone never said it was done", () -> told.shown().contains(Done.CLONED.getOutcome()));
        assertEquals("the clone kept the password in its remote", address.replace("tester:secret@", ""), mustGit(root.resolve("CHECKOUT"), "remote", "get-url", "origin").trim());
    }

    // UC-TREE-PANEL-003
    public void testACloneWithNoAddressOrNoNameIsRefusedAndNothingIsCloned() {
        new CloneTestProject(getProject(), " ", "CHECKOUT", aTreePanel()).execute();
        new CloneTestProject(getProject(), aRepositoryHoldingATestProject(), "", aTreePanel()).execute();

        assertEquals("a clone without an address or a name was not refused", List.of(Bundle.message("clone.error.missing"), Bundle.message("clone.error.missing")), said(Bundle.message("clone.error.title")));
        assertFalse("something was cloned", Files.exists(root.resolve("CHECKOUT")));
    }

    // UC-TREE-PANEL-003, Rule-SHARE-062
    public void testACloneGitRefusesSaysWhatGitSaid() {
        new CloneTestProject(getProject(), elsewhere.resolve("missing.git").toUri().toString(), "CHECKOUT", aTreePanel()).execute();

        Await.until("a clone Git refused was not reported", () -> !said(Bundle.message("clone.failed.title")).isEmpty());
        assertFalse("a refused clone left a test project behind", nodes().isTaken(root.resolve("CHECKOUT"), Optional.empty()));
    }

    // Rule-TREE-PANEL-107
    public void testACloneIsNumberedWhenTheTestinFolderAlreadyHasItsName() {
        aTestProjectAt(root.resolve("CHECKOUT"));
        readEverything();

        assertEquals("a clone took the name of a test project already there", "CHECKOUT2", CloneTestProject.nameFor(getProject(), "https://github.com/team/checkout-test-cases.git"));
        assertEquals("a clone whose name is free was numbered", "PAYMENTS", CloneTestProject.nameFor(getProject(), "https://github.com/team/payments.git"));
    }
}
