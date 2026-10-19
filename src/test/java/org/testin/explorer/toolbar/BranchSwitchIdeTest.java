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

package org.testin.explorer.toolbar;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.explorer.TreePanel;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComboBox;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BranchSwitchIdeTest extends AbstractOpenEditorsIdeTest {

    private @NotNull String rootWas = "";

    private TestProjectNode testProject;

    private TestSetNode login;

    private TestSetNode checkout;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static boolean gitOnThePath() {
        try {
            return new ProcessBuilder("git", "--version").start().waitFor() == 0;
        } catch (final IOException ex) {
            return false;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static @NotNull String git(final @NotNull Path in, final @NotNull String... arguments) {
        final @NotNull List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        try {
            final @NotNull Process process = new ProcessBuilder(command).directory(in.toFile()).redirectErrorStream(true).start();
            final @NotNull String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) throw new AssertionError(String.join(" ", command) + " failed: " + output);
            return output.trim();
        } catch (final IOException ex) {
            throw new AssertionError(String.join(" ", command) + " could not run: " + ex.getMessage(), ex);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(String.join(" ", command) + " was interrupted", ex);
        }
    }

    private static void writeUncommitted(final @NotNull Path file) {
        try {
            Files.writeString(file, "not committed");
        } catch (final IOException ex) {
            throw new AssertionError("could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void setUp() {
        super.setUp();
        assertTrue("Git is not on the PATH, so a branch cannot be switched", gitOnThePath());
        rootWas = settings().rootTestinPath;
        settings().rootTestinPath = root.toString();

        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        checkout = made.testSet(testProject.getTestCasesFolder(), "Checkout");
        made.testCase(checkout);
        bound().choose("NAFATH");

        git(testProject.getPath(), "init", "--initial-branch=main");
        git(testProject.getPath(), "config", "user.name", "Testin Test");
        git(testProject.getPath(), "config", "user.email", "testin@example.invalid");
        git(testProject.getPath(), "add", "-A");
        git(testProject.getPath(), "commit", "-m", "first");
        git(testProject.getPath(), "checkout", "-b", "feature");
        git(testProject.getPath(), "rm", "-r", "-q", testProject.getPath().relativize(login.getPath()).toString());
        git(testProject.getPath(), "commit", "-m", "no login");
        git(testProject.getPath(), "checkout", "-q", "main");

        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
    }

    @Override
    public void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        bound().choose("");
        settings().rootTestinPath = rootWas;
        super.tearDown();
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private @NotNull String branchOnDisk() {
        return git(testProject.getPath(), "rev-parse", "--abbrev-ref", "HEAD");
    }

    private @NotNull JComboBox<?> branchBox() {
        final @NotNull BranchSelector selector = new BranchSelector(getProject(), Optional.of(testProject));
        final @NotNull JComboBox<?> box = (JComboBox<?>) selector.getComponent();
        Await.until("the branch box never listed the branches", () -> box.isEnabled() && box.getItemCount() == 2);
        return box;
    }

    // Rule-TREE-PANEL-108
    public void testTheBranchBoxAppearsWhenTheTestProjectIsARepository() {
        assertTrue("the branch box is hidden on a test project that is a Git repository", branchBox().isVisible());

        final @NotNull TestProjectNode notARepository = new NodesOnDisk(getProject()).testProject(root.resolve("Plain"));
        final @NotNull BranchSelector selector = new BranchSelector(getProject(), Optional.of(notARepository));
        assertFalse("the branch box shows on a test project that is not a Git repository", selector.getComponent().isVisible());
    }

    // Rule-TREE-PANEL-086
    public void testASwitchClosesTheEditorWhoseNodeTheNewBranchDoesNotHave() {
        opened(login);
        opened(checkout);

        branchBox().setSelectedItem("feature");

        Await.until("the switch did not check the branch out", () -> branchOnDisk().equals("feature"));
        Await.until("the editor on a test set the new branch does not have is still open", () -> openOn(login).isEmpty());
        assertEquals("the editor on a test set the new branch has was closed", 1, openOn(checkout).size());
    }

    // Rule-TREE-PANEL-085
    public void testSwitchingWithUncommittedChangesAsksFirstAndKeepsThem() {
        final @NotNull Path notes = testProject.getPath().resolve("notes.txt");
        writeUncommitted(notes);

        branchBox().setSelectedItem("feature");

        Await.until("switching with uncommitted changes did not ask first", () -> ShownDialog.isOpen(getProject(), ConfirmDialog.class));
        assertEquals("the branch was switched before the tester answered", "main", branchOnDisk());

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("switching anyway did not switch", () -> branchOnDisk().equals("feature"));
        assertTrue("switching lost the uncommitted change", Files.exists(notes));
    }

    // Rule-TREE-PANEL-132
    public void testTheBranchBoxTakesTheKeyboardAsAListAndIsHeardAsBranch() {
        final @NotNull JComboBox<?> box = branchBox();

        assertEquals("a screen reader does not hear the branch box as Branch", Bundle.message("branch.box"), box.getAccessibleContext().getAccessibleName());
        assertEquals("the arrow keys choose a branch as they move instead of only moving through the list", true, box.getClientProperty("JComboBox.isTableCellEditor"));
        assertEquals("the branch box does not show the branch checked out", "main", Objects.toString(box.getSelectedItem(), ""));

        box.setSelectedItem("feature");

        Await.until("choosing a branch in the box did not switch to it", () -> branchOnDisk().equals("feature"));
    }
}
