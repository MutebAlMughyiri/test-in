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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.NodeCode;
import org.testin.editor.open.TestinEditors;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreatedNode {
    // UC-TREE-PANEL-007, UC-TREE-PANEL-009, Rule-TREE-PANEL-026, Rule-CODEGEN-023
    public static void announce(final @NotNull Project project, final @NotNull AbstractNode node) {
        Services.getInstance(project, Notifier.class).softShow(project, Done.CREATED);

        if (node.isOpenableInEditor()) Services.getInstance(project, TestinEditors.class).open(node);

        NodeCode.of(node.getType()).getCreated().execute(project, node);
    }
}
