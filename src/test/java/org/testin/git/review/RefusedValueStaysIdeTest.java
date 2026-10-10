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

package org.testin.git.review;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.TestinLog;
import org.testin.notifications.Refused;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Html;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import java.util.ArrayList;
import java.util.List;

public class RefusedValueStaysIdeTest extends BasePlatformTestCase {

    private static @NotNull List<SimpleTextAttributes> hintAttributesOf(final @NotNull ExtendableTextField field) {
        final @NotNull List<SimpleTextAttributes> attributes = new ArrayList<>();
        final SimpleColoredComponent.@NotNull ColoredIterator fragments = field.getEmptyText().getComponent().iterator();
        while (fragments.hasNext()) {
            fragments.next();
            attributes.add(fragments.getTextAttributes());
        }
        return attributes;
    }

    private @NotNull ExtendableTextField theUrlField() {
        return Drawn.components(ShownDialog.popup(getProject(), RemoteUrlDialog.class).getContent()).stream()
                .filter(ExtendableTextField.class::isInstance).map(ExtendableTextField.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no field"));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    public void testAnEmptyFieldIsMarkedAsTheOneHoldingTheDialogOpen() {
        final @NotNull List<String> taken = new ArrayList<>();
        new RemoteUrlDialog(getProject(), "origin", taken::add).show();
        try {
            ShownDialog.press(getProject(), RemoteUrlDialog.class, Shortcuts.Enter);

            assertTrue("the dialog closed over an empty field", ShownDialog.isOpen(getProject(), RemoteUrlDialog.class));
            assertEquals("an empty value was taken", List.of(), taken);
            assertTrue("the empty field is not marked as the one holding the dialog open", hintAttributesOf(theUrlField()).contains(SimpleTextAttributes.ERROR_ATTRIBUTES));
        } finally {
            ShownDialog.popup(getProject(), RemoteUrlDialog.class).cancel();
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    public void testARefusedValueIsRefusedInOneSentenceNamingItAndStaysInTheField() {
        final @NotNull List<String> taken = new ArrayList<>();
        final @NotNull TestinLog said = TestinLog.fromNow(getTestRootDisposable());
        new RemoteUrlDialog(getProject(), "origin", taken::add).show();
        try {
            theUrlField().setText("my team's repository");
            ShownDialog.press(getProject(), RemoteUrlDialog.class, Shortcuts.Enter);

            assertTrue("the dialog closed over a refused value", ShownDialog.isOpen(getProject(), RemoteUrlDialog.class));
            assertEquals("a refused value was taken", List.of(), taken);
            assertEquals("the refused value is no longer in the field", "my team's repository", theUrlField().getText());

            final @NotNull String sentence = Html.ofText(Refused.NOT_A_REPOSITORY_URL.about("my team's repository"));
            Await.until("the dialog did not say why it refused the value, naming it", () -> said.written().contains(sentence));
        } finally {
            ShownDialog.popup(getProject(), RemoteUrlDialog.class).cancel();
        }
    }
}
