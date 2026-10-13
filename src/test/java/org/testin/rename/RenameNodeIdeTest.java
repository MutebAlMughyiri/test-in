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

package org.testin.rename;

import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.TreeGesture;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.List;

public class RenameNodeIdeTest extends AbstractCodegenIdeTest {

    private static @NotNull String loginClass() {
        return "nafath." + NameSanitizer.className("Login");
    }

    private static @NotNull String signInClass() {
        return "nafath." + NameSanitizer.className("Sign in");
    }

    private static void laidOut(final @NotNull Container container) {
        container.setSize(container.getPreferredSize());
        for (final Container each : UIUtil.uiTraverser(container).filter(Container.class)) each.doLayout();
    }

    private static int topOf(final @NotNull Component component, final @NotNull Container within) {
        return SwingUtilities.convertPoint(component.getParent(), component.getLocation(), within).y;
    }

    private static @NotNull Component readingInAnyCapitals(final @NotNull Container container, final @NotNull String text) {
        return Drawn.components(container).stream()
                .filter(component -> Drawn.text(component).equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nothing on the dialog reads \"" + text + "\": " + Drawn.words(container)));
    }

    @Override
    protected void setUp() {
        super.setUp();
        undoHistories().forget(UndoScope.TREE);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), RenameDialog.class);
        undoHistories().forget(UndoScope.TREE);
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private void renamedToSignIn(final @NotNull AbstractNode node) {
        TreeGesture.pressed(getProject(), new RenameAction(), List.of(node));
        ShownDialog.typed(getProject(), RenameDialog.class, "Sign in");
        ShownDialog.press(getProject(), RenameDialog.class, Shortcuts.Enter);
        Await.until(node.getName() + " was not renamed to Sign in", () -> nodes().nodeExists(node.getPath().resolveSibling("Sign in")));
    }

    // Rule-TREE-PANEL-036
    public void testRenamingATestSetRenamesItsClass() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        assertTrue("the test set was given no class", generatedClass(loginClass()).isPresent());

        renamedToSignIn(login);

        Await.until("renaming the test set did not rename its class", () -> generatedClass(signInClass()).isPresent());
        assertTrue("the class under the old name is still there", generatedClass(loginClass()).isEmpty());
    }

    // Rule-TREE-PANEL-037
    public void testARenameIsUndone() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull Path wasAt = login.getPath();

        renamedToSignIn(login);
        Await.until("the rename never went onto the tree's history", () -> undoHistories().canUndo(UndoScope.TREE));

        assertTrue("undoing the rename was refused", undoHistories().undo(UndoScope.TREE));

        Await.until("undoing the rename did not give the test set its name back", () -> nodes().nodeExists(wasAt) && !nodes().nodeExists(wasAt.resolveSibling("Sign in")));
        Await.until("undoing the rename did not give the class its name back", () -> generatedClass(loginClass()).isPresent());
    }

    // Rule-TREE-PANEL-123
    public void testTheDialogSaysWhatItRenamesAndWhereAboveTheField() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        TreeGesture.pressed(getProject(), new RenameAction(), List.of(login));
        final @NotNull JComponent content = ShownDialog.content(getProject(), RenameDialog.class);
        laidOut(content);

        final @NotNull Component renaming = readingInAnyCapitals(content, Bundle.message("caption.renaming"));
        final @NotNull Component name = readingInAnyCapitals(content, "Login");
        final @NotNull Component in = readingInAnyCapitals(content, Bundle.message("caption.in"));
        final @NotNull JTextComponent field = Drawn.first(content, JTextComponent.class, JTextComponent::isEditable);

        assertTrue("the place is not said: " + Drawn.words(content), Drawn.holds(Drawn.words(content), "Test Cases"));
        ShownDialog.typed(getProject(), RenameDialog.class, "Sign in");
        assertTrue("the name being replaced cannot be read once typing has begun: " + Drawn.words(content), Drawn.holds(Drawn.words(content), "Login"));

        for (final Component above : List.of(renaming, name, in)) {
            assertTrue("'" + Drawn.text(above) + "' is not above the field", topOf(above, content) < topOf(field, content));
        }
    }
}
