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

package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.LayeredIcon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.Icon;
import java.util.List;

@Getter
@AllArgsConstructor
public enum Automated {
    UNKNOWN(
            Bundle.message("action.Testin.NavigateToTestMethod.text"),
            AllIcons.Nodes.Class
    ),

    WRITTEN(
            Bundle.message("automated.written"),
            AllIcons.Nodes.Class
    ),

    MISSING(
            Bundle.message("automated.missing"),
            LayeredIcon.create(AllIcons.Nodes.Class, AllIcons.Nodes.ErrorMark)
    ),

    // Rule-CODEGEN-002
    NONE(
            Bundle.message("automated.none"),
            AllIcons.Nodes.AbstractClass
    );

    public static final @NotNull List<Automated> FILTERABLE = List.of(WRITTEN, MISSING, NONE);

    private final @NotNull String label;

    private final @NotNull Icon icon;
}
