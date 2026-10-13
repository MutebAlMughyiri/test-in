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

package org.testin.model.node;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.NodeType;
import org.testin.model.markers.Marker;

import javax.swing.Icon;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public abstract class AbstractNode {
    @NonNull
    @Builder.Default
    private String name = "";

    @NonNull
    @Builder.Default
    private Path path = Path.of("");

    @ToString.Exclude
    private @Nullable AbstractNode parent;

    public @NotNull List<String> treePath() {
        return selfAndAncestors().reversed().stream()
                .map(AbstractNode::getName)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    public @NotNull List<AbstractNode> selfAndAncestors() {
        final @NotNull List<AbstractNode> chain = new ArrayList<>();
        for (AbstractNode current = this; current != null; current = current.getParent()) {
            chain.add(current);
        }
        return chain;
    }

    @NonNull
    public abstract Marker getMarker();

    // Rule-INTERNAL-116
    @Override
    public final boolean equals(final @Nullable Object other) {
        return other instanceof AbstractNode node && node.getClass() == getClass() && node.path.equals(path);
    }

    @Override
    public final int hashCode() {
        return path.hashCode();
    }

    public int getOrder() {
        return getMarker().getOrder();
    }

    public boolean isOrderable() {
        return false;
    }

    @NonNull
    public String getMarkerFileName() {
        return getType().getMarker();
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-009
    public @NotNull List<NodeType> childKinds() {
        return List.of();
    }

    public boolean canCreateChildren() {
        return !childKinds().isEmpty();
    }

    public boolean isRenamable() {
        return true;
    }

    // Rule-TREE-PANEL-100
    public @NotNull List<AbstractNode> fixedChildren() {
        return List.of();
    }

    public boolean isTestCaseContainer() {
        return false;
    }

    // Rule-TREE-PANEL-129, Rule-EDITOR-PANEL-260
    public @NotNull String nameUnder(final @NotNull Path folder) {
        return folder.relativize(path).toString().replace(folder.getFileSystem().getSeparator(), " / ");
    }

    // Rule-TREE-PANEL-009, Rule-PRODUCT-011
    public @NotNull Optional<String> whySignedOff() {
        return Optional.empty();
    }

    public boolean isOpenableInEditor() {
        return false;
    }

    public boolean isTransferable() {
        return true;
    }

    public boolean isRemovable() {
        return true;
    }

    public boolean isTransferTarget() {
        return getType().acceptsAnything();
    }

    public boolean acceptsTransferred(final @NotNull AbstractNode source) {
        return getType().accepts(source.getType());
    }

    public boolean isRetired() {
        return false;
    }

    // UC-TREE-PANEL-001, Rule-INTERNAL-072
    public @NotNull Icon iconShownInTree() {
        return getType().getIcon();
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-099
    public @NotNull String statusShownInTree() {
        return getMarker().status().isActive() ? "" : getMarker().getStatusLabel();
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-127
    public boolean mayHaveChildren() {
        return !fixedChildren().isEmpty() || getType().acceptsAnything();
    }

    public boolean holdsTestCases() {
        return false;
    }

    @NotNull
    public abstract NodeType getType();
}
