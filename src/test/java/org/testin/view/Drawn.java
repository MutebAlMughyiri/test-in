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
package org.testin.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.view.details.DetailsTab;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Drawn {

    public static @NotNull JBPanel<?> detailsTab(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> treePath) {
        return detailsTab(project, testCase, runItem, treePath, OptionalInt.empty());
    }

    public static @NotNull JBPanel<?> detailsTab(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull Optional<RunItem> runItem, final @NotNull List<String> treePath, final @NotNull OptionalInt shownLine) {
        final @NotNull JBPanel<?> tab = new JBPanel<>();
        new DetailsTab().load(project, tab, Optional.of(testCase), runItem, treePath, shownLine, _ -> {
        });
        return tab;
    }

    public static @NotNull List<Component> components(final @NotNull Container container) {
        final @NotNull List<Component> found = new ArrayList<>();
        for (final Component child : container.getComponents()) {
            found.add(child);
            if (child instanceof final Container inner) found.addAll(components(inner));
        }
        return found;
    }

    public static <T> @NotNull T first(final @NotNull Component root, final @NotNull Class<T> kind) {
        return first(root, kind, _ -> true);
    }

    public static <T> @NotNull T first(final @NotNull Component root, final @NotNull Class<T> kind, final @NotNull Predicate<? super T> which) {
        return Optional.ofNullable(UIUtil.uiTraverser(root).filter(kind).filter(which::test).first()).orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " is drawn"));
    }

    public static @NotNull String text(final @NotNull Component component) {
        final String text = switch (component) {
            case final AbstractButton button -> button.getText();
            case final JLabel label -> label.getText();
            case final JTextComponent area -> area.getText();
            default -> "";
        };
        return Objects.requireNonNullElse(text, "").trim();
    }

    public static @NotNull String hovering(final @NotNull JComponent component) {
        return StringUtil.unescapeXmlEntities(StringUtil.removeHtmlTags(Objects.toString(component.getToolTipText(), ""))).trim();
    }

    public static @NotNull List<String> words(final @NotNull Container container) {
        return components(container).stream().map(Drawn::text).filter(text -> !text.isEmpty()).toList();
    }

    public static boolean holds(final @NotNull List<String> words, final @NotNull String text) {
        return words.stream().anyMatch(word -> word.contains(text));
    }

    public static @NotNull Component reading(final @NotNull Container container, final @NotNull String text) {
        return components(container).stream()
                .filter(component -> text(component).equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nothing on the panel reads \"" + text + "\": " + words(container)));
    }
}
