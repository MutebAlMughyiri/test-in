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

package org.testin.creator;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Said;
import org.testin.creator.dialogs.CreateTestProjectDialog;
import org.testin.notifications.Refused;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;

public class CreateTestProjectDialogIdeTest extends AbstractIdeTest {

    private final @NotNull List<String> created = new ArrayList<>();

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), CreateTestProjectDialog.class);
        super.tearDown();
    }

    private void typedAndConfirmed(final @NotNull String typed) {
        ShownDialog.open(getProject(), CreateTestProjectDialog.class, () -> new CreateTestProjectDialog(getProject(), created::add).show());
        ShownDialog.typed(getProject(), CreateTestProjectDialog.class, typed);
        ShownDialog.press(getProject(), CreateTestProjectDialog.class, Shortcuts.Enter);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-095
    public void testANameATestProjectCanTakeIsHandedOnAndTheDialogCloses() {
        typedAndConfirmed("Checkout");

        assertEquals("the name typed was not handed on", List.of("Checkout"), created);
        assertFalse("the dialog stayed open after its name was taken", ShownDialog.isOpen(getProject(), CreateTestProjectDialog.class));
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-107
    public void testARepositoryAddressIsHandedOnToBeCloned() {
        typedAndConfirmed("https://github.com/team/checkout-test-cases.git");

        assertEquals("the address typed was not handed on", List.of("https://github.com/team/checkout-test-cases.git"), created);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-095
    public void testAPathIsRefusedAsAPathAndANameJavaRefusesAsAName() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        typedAndConfirmed("../Checkout");
        ShownDialog.typed(getProject(), CreateTestProjectDialog.class, "Class");
        ShownDialog.press(getProject(), CreateTestProjectDialog.class, Shortcuts.Enter);

        assertEquals("a name a test project cannot take was handed on", List.of(), created);
        assertEquals("the tester is not told which refusal they met", List.of(Refused.NOT_ONE_FOLDER.about("../Checkout"), Refused.NOT_A_JAVA_NAME.about("Class")), said.shown());
        assertTrue("the dialog closed over a name it refused", ShownDialog.isOpen(getProject(), CreateTestProjectDialog.class));
    }
}
