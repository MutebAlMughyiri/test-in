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

package org.testin.explorer.tree;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RevealInTheTreeIdeTest extends AbstractReadTheRootIdeTest {

    private static @NotNull Optional<Path> selectedIn(final @NotNull TestProjectTree tree) {
        return Optional.ofNullable(tree.getMainTree().getSelectionPath()).flatMap(TreeValues::directoryAt).map(AbstractNode::getPath);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    public void testAPathRevealedIsSelectedInTheTreeAndWhatWaitsForItRunsAfter() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("CHECKOUT"));
        final @NotNull Path payments = marked(theTestCasesOf(checkout).resolve("Payments"), NodeType.TEST_SET_PACKAGE);
        final @NotNull Path card = marked(payments.resolve("Card"), NodeType.TEST_SET);
        readEverything();
        Services.getInstance(getProject(), BoundTestProject.class).choose("CHECKOUT");
        final @NotNull TestProjectTree tree = new TestProjectTree(getProject());
        Disposer.register(getTestRootDisposable(), tree);
        tree.refresh();
        final @NotNull AtomicInteger waited = new AtomicInteger();

        tree.reveal(card, waited::incrementAndGet);

        Await.until("the path revealed was not selected in the tree", () -> waited.get() == 1);
        assertEquals("the tree did not select the path revealed, under the package it is in", Optional.of(card), selectedIn(tree));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    public void testAPathRevealedPastAnotherPackageIsSelectedUnderItsOwn() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("CHECKOUT"));
        final @NotNull Path accounts = marked(theTestCasesOf(checkout).resolve("Accounts"), NodeType.TEST_SET_PACKAGE);
        marked(accounts.resolve("Sign up"), NodeType.TEST_SET);
        final @NotNull Path card = marked(marked(theTestCasesOf(checkout).resolve("Payments"), NodeType.TEST_SET_PACKAGE).resolve("Card"), NodeType.TEST_SET);
        readEverything();
        Services.getInstance(getProject(), BoundTestProject.class).choose("CHECKOUT");
        final @NotNull TestProjectTree tree = new TestProjectTree(getProject());
        Disposer.register(getTestRootDisposable(), tree);
        tree.refresh();
        final @NotNull AtomicInteger waited = new AtomicInteger();

        tree.reveal(card, waited::incrementAndGet);

        Await.until("the path revealed past another package was not selected in the tree", () -> waited.get() == 1);
        assertEquals("the tree did not select the path revealed, under its own package", Optional.of(card), selectedIn(tree));
    }
}
