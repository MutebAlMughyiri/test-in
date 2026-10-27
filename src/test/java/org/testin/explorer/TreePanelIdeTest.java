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

package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.TreeGesture;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.CollapseTreeAction;
import org.testin.explorer.toolbar.ExpandTreeAction;
import org.testin.explorer.toolbar.SaveTestinYmlAction;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.TestProjectStatus;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.SelectTestProjectAction;
import org.testin.util.Bundle;

import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

public class TreePanelIdeTest extends AbstractReadTheRootIdeTest {

    private static @NotNull List<Object> rowsOf(final @NotNull SimpleTree tree) {
        PlatformTestUtil.waitWhileBusy(tree);
        PlatformTestUtil.expandAll(tree);
        return IntStream.range(0, tree.getRowCount())
                .mapToObj(row -> TreeValues.valueOf(tree.getPathForRow(row).getLastPathComponent(), Object.class).orElse(""))
                .toList();
    }

    private static @NotNull List<Object> settledRowsOf(final @NotNull TreePanel panel) {
        final @NotNull SimpleTree tree = panel.getTestProjectTree().getMainTree();
        Await.until("the tree never showed Checkout", () -> !rowsOf(tree).isEmpty() && rowsOf(tree).getFirst() instanceof final AbstractNode top && top.getName().equals("Checkout"));
        return rowsOf(tree);
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private @NotNull TreePanel aPanel() {
        final @NotNull TreePanel panel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), panel);
        return panel;
    }

    private @NotNull TreePanel aPanelShowingTheTree() {
        final @NotNull TreePanel panel = aPanel();
        Await.until("the panel never showed the tree", panel::showsTree);
        return panel;
    }

    private @NotNull TreePanel aPanelShowingTheWelcome() {
        final @NotNull TreePanel panel = aPanel();
        Await.until("the panel never showed the welcome screen", () -> !panel.showsTree());
        return panel;
    }

    private @NotNull List<AnAction> titleBarOf(final @NotNull TreePanel panel) {
        return new TreePanelToolbar().create(getProject(), panel);
    }

    private @NotNull Presentation updated(final @NotNull AnAction action) {
        return TreeGesture.updated(getProject(), action, List.of());
    }

    private void assertGrayWithReason(final @NotNull AnAction action) {
        final @NotNull Presentation shown = updated(action);
        assertFalse(action.getClass().getSimpleName() + " is not gray", shown.isEnabled());
        assertFalse(action.getClass().getSimpleName() + " is gray and does not say why", Objects.requireNonNullElse(shown.getDescription(), "").isBlank());
    }

    // Rule-TREE-PANEL-001, Rule-TREE-PANEL-012
    public void testThePanelShowsOnlyTheBoundTestProjectAndNoTestCase() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));
        aTestProjectAt(root.resolve("Payments"));
        aTestCaseIn(marked(theTestCasesOf(checkout).resolve("Login"), NodeType.TEST_SET));
        readEverything();
        bound().choose("Checkout");

        final @NotNull List<Object> rows = settledRowsOf(aPanelShowingTheTree());

        assertEquals("the tree does not show exactly one test project: " + rows, 1, rows.stream().filter(TestProjectNode.class::isInstance).count());
        assertTrue("the tree lists a test project it is not bound to: " + rows, rows.stream().noneMatch(row -> row instanceof final AbstractNode node && node.getName().equals("Payments")));
        assertTrue("a test case is a node in the tree: " + rows, rows.stream().allMatch(AbstractNode.class::isInstance));
        assertTrue("the test set holding the test case is not in the tree: " + rows, rows.stream().anyMatch(row -> row instanceof final AbstractNode node && node.getName().equals("Login")));
    }

    // Rule-TREE-PANEL-015
    public void testTheOnlyTestProjectIsBoundWithoutAsking() {
        aTestProjectAt(root.resolve("Checkout"));
        readEverything();

        aPanel();

        Await.until("the only test project in the Testin folder was not bound", () -> bound().name().equals("Checkout"));
    }

    // Rule-TREE-PANEL-015
    public void testOneOfSeveralTestProjectsIsNotBoundWithoutAsking() {
        aTestProjectAt(root.resolve("Checkout"));
        aTestProjectAt(root.resolve("Payments"));
        readEverything();

        aPanelShowingTheWelcome();

        assertEquals("one of two test projects was bound without asking", "", bound().name());
    }

    // Rule-TREE-PANEL-064
    public void testAnInactiveTestProjectIsOpenedAndBoundAndHoldsNothing() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));
        aTestCaseIn(marked(theTestCasesOf(checkout).resolve("Login"), NodeType.TEST_SET));
        readEverything();
        final @NotNull TestProjectNode testProject = (TestProjectNode) nodes().find(checkout).orElseThrow();
        WriteAction.runAndWait(() -> {
            testProject.getMarker().setStatus(TestProjectStatus.INACTIVE);
            nodes().persistMarker(testProject);
        });
        readEverything();
        bound().choose("Checkout");

        final @NotNull List<Object> rows = settledRowsOf(aPanelShowingTheTree());

        assertEquals("an inactive test project is no longer bound", "Checkout", bound().name());
        assertEquals("an inactive test project is drawn with what it holds: " + rows, 1, rows.size());
    }

    // Rule-TREE-PANEL-097
    public void testOpeningThePanelPutsTheKeyboardInTheTree() {
        aTestProjectAt(root.resolve("Checkout"));
        readEverything();
        bound().choose("Checkout");
        final @NotNull TreePanel panel = aPanelShowingTheTree();
        final @NotNull Content content = ContentFactory.getInstance().createContent(panel.getPanel(), null, false);

        panel.showIn(content);

        assertSame("opening the panel does not give the keyboard to the tree", panel.getTestProjectTree().getMainTree(), content.getPreferredFocusableComponent());
    }

    // Rule-TREE-PANEL-101
    public void testUnderTheWelcomeScreenTheTitleBarAimsAtThePanelAndExpandAndCollapseSayWhyTheyAreGray() {
        final @NotNull TreePanel panel = aPanelShowingTheWelcome();
        final @NotNull Content content = ContentFactory.getInstance().createContent(panel.getPanel(), null, false);

        panel.showIn(content);

        assertSame("under the welcome screen the title bar aims at a hidden component", panel.getPanel(), content.getPreferredFocusableComponent());
        for (final AnAction treeOnly : List.of(new ExpandTreeAction(panel), new CollapseTreeAction(panel))) {
            final @NotNull Presentation shown = updated(treeOnly);
            assertFalse(treeOnly.getClass().getSimpleName() + " works with no tree to act on", shown.isEnabled());
            assertEquals(treeOnly.getClass().getSimpleName() + " does not say why it is gray", Bundle.message("toolbar.tree.hidden.description"), shown.getDescription());
        }
    }

    // Rule-TREE-PANEL-101
    public void testWithTheTreeShownExpandAndCollapseWork() {
        aTestProjectAt(root.resolve("Checkout"));
        readEverything();
        bound().choose("Checkout");
        final @NotNull TreePanel panel = aPanelShowingTheTree();

        for (final AnAction treeOnly : List.of(new ExpandTreeAction(panel), new CollapseTreeAction(panel))) {
            assertTrue(treeOnly.getClass().getSimpleName() + " is gray with the tree shown", updated(treeOnly).isEnabled());
        }
    }

    // Rule-TREE-PANEL-090
    public void testEveryTitleBarButtonSaysWhatItDoes() {
        final @NotNull TreePanel panel = aPanel();

        for (final AnAction button : titleBarOf(panel)) {
            final @NotNull Presentation template = button.getTemplatePresentation();
            assertFalse(button.getClass().getSimpleName() + " has no name to show on hover", Objects.requireNonNullElse(template.getText(), "").isBlank());
            assertFalse(button.getClass().getSimpleName() + " does not say what it does on hover", Objects.requireNonNullElse(template.getDescription(), "").isBlank());
        }
    }

    // Rule-TREE-PANEL-115
    public void testWithoutATestinFolderSelectAndNewAreGrayAndSayWhy() {
        settings().rootTestinPath = "";
        final @NotNull TreePanel panel = aPanel();

        assertGrayWithReason(new SelectTestProjectAction(getProject(), panel));
        assertGrayWithReason(new CreateTestProjectAction(getProject(), panel));
        assertGrayWithReason(new SaveTestinYmlAction(getProject()));
        assertEquals(Bundle.message("yml.save.disabled.no.test.project"), updated(new SaveTestinYmlAction(getProject())).getDescription());

        for (final AnAction button : titleBarOf(panel)) {
            if (button instanceof SelectTestProjectAction || button instanceof CreateTestProjectAction || button instanceof SaveTestinYmlAction
                    || button instanceof ExpandTreeAction || button instanceof CollapseTreeAction) continue;
            assertTrue(button.getClass().getSimpleName() + " is gray", updated(button).isEnabled());
        }
    }

    // Rule-TREE-PANEL-115
    public void testWithATestProjectOpenNothingOnTheTitleBarIsGray() {
        aTestProjectAt(root.resolve("Checkout"));
        readEverything();
        bound().choose("Checkout");
        final @NotNull TreePanel panel = aPanelShowingTheTree();

        for (final AnAction button : titleBarOf(panel)) {
            assertTrue(button.getClass().getSimpleName() + " is gray with a test project open: " + updated(button).getDescription(), updated(button).isEnabled());
        }
    }

    // Rule-TREE-PANEL-083
    public void testASecondRefreshWaitsAndTwoWaitingAreOneReportingTheLast() {
        aTestProjectAt(root.resolve("Checkout"));
        readEverything();
        bound().choose("Checkout");
        final @NotNull TreePanel panel = aPanelShowingTheTree();
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        panel.reindex("First refresh");
        panel.reindex("Second refresh");
        panel.reindex("Third refresh");

        Await.until("the waiting refresh never ran", () -> balloons.shown().contains("Third refresh"));
        final @NotNull List<String> heard = balloons.shown();

        assertEquals("refreshes did not run one at a time, the waiting two as one reporting the last: " + heard, List.of("First refresh", "Third refresh"), heard.stream().filter(said -> said.endsWith("refresh")).toList());
    }

    // Rule-TREE-PANEL-006
    public void testSwingsOwnCutCopyAndPasteOnTheTreeDoNothing() {
        final @NotNull JTree tree = aPanel().getTestProjectTree().getMainTree();
        CopyPasteManager.getInstance().setContents(new StringSelection("left alone"));
        try {
            for (final Action swingOwn : List.of(TransferHandler.getCutAction(), TransferHandler.getCopyAction(), TransferHandler.getPasteAction())) {
                final @NotNull Action answered = Objects.requireNonNull(tree.getActionMap().get(swingOwn.getValue(Action.NAME)), "the tree has no " + swingOwn.getValue(Action.NAME));
                assertNotSame("the tree still answers Swing's own " + swingOwn.getValue(Action.NAME), swingOwn, answered);
                answered.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, String.valueOf(swingOwn.getValue(Action.NAME))));
            }

            assertEquals("Swing's own clipboard keys on the tree changed the clipboard", "left alone", CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
        } finally {
            CopyPasteManager.getInstance().setContents(new StringSelection(""));
        }
    }

    // Rule-TREE-PANEL-014
    public void testThePanelNeverOpensOnItsOwnWhenTheIdeStarts() {
        final @NotNull Element plugin = pluginXml();
        final @NotNull Optional<Element> declared = plugin.getChildren("extensions").stream()
                .flatMap(extensions -> extensions.getChildren("toolWindow").stream())
                .filter(window -> "testin.tree".equals(window.getAttributeValue("id")))
                .findFirst();

        assertTrue("the tree panel is not declared", declared.isPresent());
        assertEquals("the tree panel opens on its own when the IDE starts", "true", declared.orElseThrow().getAttributeValue("doNotActivateOnStart"));
    }

    private @NotNull Element pluginXml() {
        try (InputStream xml = Objects.requireNonNull(TreePanel.class.getResourceAsStream("/META-INF/plugin.xml"), "plugin.xml is not on the class path")) {
            return JDOMUtil.load(xml);
        } catch (final IOException | JDOMException ex) {
            throw new AssertionError("could not read plugin.xml: " + ex.getMessage(), ex);
        }
    }
}
