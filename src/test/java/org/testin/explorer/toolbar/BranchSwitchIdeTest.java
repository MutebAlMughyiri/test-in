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
import org.testin.Said;
import org.testin.explorer.TreePanel;
import org.testin.git.review.PendingChangesDialog;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JButton;
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
        return boxOf(listingBoth());
    }

    private @NotNull BranchSelector listingBoth() {
        final @NotNull BranchSelector selector = new BranchSelector(getProject(), Optional.of(testProject));
        Await.until("the branch box never listed the branches", () -> boxOf(selector).isEnabled() && boxOf(selector).getItemCount() == 2);
        return selector;
    }

    private static @NotNull JComboBox<?> boxOf(final @NotNull BranchSelector selector) {
        return (JComboBox<?>) selector.getComponent();
    }

    private void changedTheLoginMarker() {
        writeUncommitted(login.getPath().resolve(login.getMarkerFileName()));
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

    // UC-TREE-PANEL-026
    public void testARepositoryWithNoBranchesSaysSoAndCannotBeOpened() {
        final @NotNull TestProjectNode empty = new NodesOnDisk(getProject()).testProject(root.resolve("Empty"));
        git(empty.getPath(), "init", "--initial-branch=main");

        final @NotNull JComboBox<?> box = boxOf(new BranchSelector(getProject(), Optional.of(empty)));

        Await.until("a repository with no branches does not say so", () -> Bundle.message("branch.none").equals(Objects.toString(box.getSelectedItem(), "")));
        assertFalse("the box of a repository with no branches can be opened", box.isEnabled());
    }

    // UC-TREE-PANEL-026
    public void testOnNoBranchAtAllTheBoxListsTheBranchesWithNoneSelected() {
        git(testProject.getPath(), "checkout", "-q", "--detach");

        final @NotNull JComboBox<?> box = branchBox();

        assertEquals("a repository on no branch shows a branch selected", -1, box.getSelectedIndex());
    }

    // UC-SHARE-014, Rule-SHARE-064
    public void testABranchStartedElsewhereIsListedWhenTheBranchesAreReadAgain() {
        final @NotNull BranchSelector selector = listingBoth();
        git(testProject.getPath(), "branch", "release");

        selector.readBranches();

        Await.until("a branch started since the box was filled is not listed after the branches are read again", () -> boxOf(selector).getItemCount() == 3);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-085
    public void testReviewChangesOpensTheReviewInsteadOfSwitching() {
        changedTheLoginMarker();
        branchBox().setSelectedItem("feature");
        Await.until("switching with uncommitted changes did not ask first", () -> ShownDialog.isOpen(getProject(), ConfirmDialog.class));

        Drawn.first(ShownDialog.content(getProject(), ConfirmDialog.class), JButton.class, button -> button.getText().equals(Bundle.message("branch.review.changes"))).doClick();
        try {
            Await.until("Review Changes did not open the review", () -> ShownDialog.isOpen(getProject(), PendingChangesDialog.class));
            assertEquals("Review Changes switched the branch", "main", branchOnDisk());
        } finally {
            ShownDialog.close(getProject(), PendingChangesDialog.class);
        }
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-085, Rule-SHARE-127
    public void testACheckoutGitRefusesPutsTheBoxBackAndKeepsTheChange() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        changedTheLoginMarker();
        final @NotNull JComboBox<?> box = branchBox();
        box.setSelectedItem("feature");
        Await.until("switching with uncommitted changes did not ask first", () -> ShownDialog.isOpen(getProject(), ConfirmDialog.class));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("a checkout Git refused was not said", () -> said.notifications().stream().anyMatch(notification -> notification.getTitle().equals(Bundle.message("git.branch.not.switched.title"))));
        Await.until("the box did not go back to the branch the tester was on", () -> "main".equals(Objects.toString(box.getSelectedItem(), "")));
        assertEquals("a refused checkout moved the branch", "main", branchOnDisk());
        try {
            assertEquals("a refused checkout lost the uncommitted change", "not committed", Files.readString(login.getPath().resolve(login.getMarkerFileName())));
        } catch (final IOException ex) {
            throw new AssertionError("could not read the changed marker: " + ex.getMessage(), ex);
        }
    }

    // UC-TREE-PANEL-026
    public void testARemoteThatCannotBeReachedKeepsTheBranchesReadAndSaysWhy() {
        git(testProject.getPath(), "remote", "add", "origin", root.resolve("nowhere").toString());
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull BranchSelector selector = listingBoth();

        selector.fetchBranches();

        Await.until("a remote that cannot be reached was not said", () -> said.notifications().stream().anyMatch(notification -> notification.getTitle().equals(Bundle.message("branch.fetch.warning.title"))));
        assertEquals("a remote that cannot be reached lost the branches read from the machine", 2, boxOf(selector).getItemCount());
    }
}
