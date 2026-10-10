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
package org.testin.testrun.form;

import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

public record TestRunNameSection(@NotNull ComponentDialogBase<TextInput> component) {
    // UC-TREE-PANEL-021
    public static @NotNull TestRunNameSection of(final @NotNull String name) {
        return new TestRunNameSection(ComponentDialogBase.textField()
                .caption(Bundle.message("test.run.form.name.caption"))
                .placeholder(Bundle.message("test.run.form.name.hint"))
                .value(name)
                .build());
    }

    public @NotNull String name() {
        return component.getComponent().getText().trim();
    }
}
