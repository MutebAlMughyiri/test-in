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

package org.testin.ui.framework;

import com.intellij.lang.Language;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.EditorFields;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogPart<C extends DialogComponent> {
    @Getter
    private final @NotNull C component;

    public static <T> @NotNull TextFieldBuilder<T> textFieldWithSelections() {
        return new TextFieldBuilder<>();
    }

    public static @NotNull TextInputBuilder textInput() {
        return new TextInputBuilder();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-221, Rule-INTERNAL-097
    public static @NotNull DialogPart<MultiLineField> multiLineField(final @NotNull Project p, final @NotNull String caption, final @NotNull String placeholder, final @NotNull String value) {
        final @NotNull MultiLineField field = new MultiLineField(p, EditorFields.createField(p), caption, placeholder);
        field.setText(value);

        return new DialogPart<>(field);
    }

    // Rule-INTERNAL-136, Rule-INTERNAL-139
    public static @NotNull DialogPart<MultiLineField> multiLineField(final @NotNull Project p, final @NotNull String caption, final @NotNull String placeholder, final int minimumLines, final int maximumLines, final @NotNull Language language, final boolean spellChecked) {
        return new DialogPart<>(new MultiLineField(p, EditorFields.createField(p, language, spellChecked), caption, placeholder, minimumLines, maximumLines));
    }

    // Rule-INTERNAL-137
    public static @NotNull DialogPart<FieldPair> fieldPair(final @NotNull Project p, final @NotNull String rememberedAs, final @NotNull JBLabel firstTitle, final @NotNull MultiLineField first, final @NotNull JBLabel secondTitle, final @NotNull MultiLineField second) {
        return new DialogPart<>(new FieldPair(p, rememberedAs, firstTitle, first, secondTitle, second));
    }

    public static @NotNull DialogPart<DialogMessage> message(final @NotNull String text) {
        return new DialogPart<>(new DialogMessage(text));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-099, Rule-INTERNAL-108
    public static @NotNull DialogPart<ConfirmCard> confirmCard(final @NotNull String text, final @NotNull List<String> from, final @NotNull List<String> to) {
        return new DialogPart<>(new ConfirmCard(text, from, to));
    }

    public static <C extends DialogComponent> @NotNull DialogPart<C> of(final @NotNull C component) {
        return new DialogPart<>(component);
    }

    public static @NotNull DialogPart<DialogSplitButton> splitButton(final @NotNull String... labels) {
        return new DialogPart<>(new DialogSplitButton(List.of(labels)));
    }

    public static @NotNull DialogPart<DialogButton> button(final @NotNull String text) {
        return button(text, List.of());
    }

    // Rule-INTERNAL-140
    public static @NotNull DialogPart<DialogButton> button(final @NotNull String text, final @NotNull List<Alternative> others) {
        return new DialogPart<>(new DialogButton(text, others));
    }

    public static @NotNull DetailsBuilder details() {
        return new DetailsBuilder();
    }

    public static @NotNull TextAreaBuilder textArea() {
        return new TextAreaBuilder();
    }

    // Rule-EDITOR-PANEL-219
    public static @NotNull DialogPart<Screenshots> screenshots(final @NotNull String caption, final @NotNull List<byte[]> stored) {
        return new DialogPart<>(new Screenshots(caption, stored));
    }

    public static @NotNull DialogPart<Picture> picture(final byte @NotNull [] png) {
        return new DialogPart<>(new Picture(png));
    }

    public static @NotNull DialogPart<ChoiceInput> choice(final @NotNull String caption, final @NotNull List<String> options, final @NotNull String selected) {
        return new DialogPart<>(new ChoiceInput(caption, List.copyOf(options), selected));
    }

    public static <T> @NotNull RadioBuilder<T> radios(final @NotNull String caption) {
        return new RadioBuilder<>(caption);
    }

    public static @NotNull TableBuilder table() {
        return new TableBuilder();
    }

    public static final class TableBuilder {
        private final @NotNull List<String> columns = new ArrayList<>();
        private final @NotNull List<Integer> widths = new ArrayList<>();

        public @NotNull TableBuilder column(final @NotNull String heading, final int width) {
            columns.add(heading);
            widths.add(width);
            return this;
        }

        public @NotNull DialogPart<SelectionTable> build() {
            return new DialogPart<>(new SelectionTable(List.copyOf(columns), List.copyOf(widths)));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class DetailsBuilder {
        private final @NotNull List<DialogDetailRow> rows = new ArrayList<>();

        public @NotNull DetailsBuilder row(final @NotNull String caption, final @NotNull String value) {
            if (!value.isBlank()) {
                rows.add(new DialogDetailRow(caption, Optional.empty(), value));
            }
            return this;
        }

        public @NotNull DetailsBuilder row(final @NotNull Icon icon, final @NotNull String value) {
            if (!value.isBlank()) {
                rows.add(new DialogDetailRow("", Optional.of(icon), value));
            }
            return this;
        }

        public @NotNull DialogPart<DialogDetails> build() {
            return new DialogPart<>(new DialogDetails(List.copyOf(rows)));
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class RadioBuilder<T> {
        private final @NotNull String caption;
        private final @NotNull List<Option<T>> options = new ArrayList<>();
        private @NotNull Optional<T> selected = Optional.empty();

        public @NotNull RadioBuilder<T> option(final @NotNull String name, final @NotNull T value) {
            options.add(new Option<>(name, value));
            return this;
        }

        public @NotNull RadioBuilder<T> options(final @NotNull List<T> values, final @NotNull Function<T, String> name) {
            values.forEach(value -> option(name.apply(value), value));
            return this;
        }

        public @NotNull RadioBuilder<T> select(final @NotNull T value) {
            this.selected = Optional.of(value);
            return this;
        }

        public @NotNull DialogPart<RadioSelection<T>> build() {
            if (options.isEmpty()) {
                throw new IllegalStateException("radios needs at least one .option(...)");
            }
            if (selected.isEmpty()) {
                throw new IllegalStateException("radios needs .select(...) with the value it opens on");
            }
            return new DialogPart<>(new RadioSelection<>(caption, List.copyOf(options), selected.orElseThrow()));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextAreaBuilder {
        private @NotNull String placeholder = "";
        private @NotNull String value = "";
        private int rows = 5;
        private boolean readOnly = false;

        public @NotNull TextAreaBuilder placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextAreaBuilder value(final @NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull TextAreaBuilder rows(final int rows) {
            this.rows = rows;
            return this;
        }

        // Rule-VIEW-PANEL-036
        public @NotNull TextAreaBuilder readOnly() {
            this.readOnly = true;
            return this;
        }

        public @NotNull DialogPart<PlainTextArea> build() {
            return new DialogPart<>(new PlainTextArea(placeholder, value, rows, readOnly));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextInputBuilder {
        private @NotNull Icon icon = DialogStyle.NO_ICON;
        private @NotNull String caption = "";
        private @NotNull String placeholder = "";
        private @NotNull String value = "";
        private @NotNull String accepts = TextInput.ANYTHING;
        private @NotNull Optional<Consumer<TextInput>> browsing = Optional.empty();

        public @NotNull TextInputBuilder icon(final @NotNull Icon icon) {
            this.icon = icon;
            return this;
        }

        public @NotNull TextInputBuilder caption(final @NotNull String caption) {
            this.caption = caption;
            return this;
        }

        public @NotNull TextInputBuilder placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextInputBuilder value(final @NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull TextInputBuilder accepting(final @NotNull String regex) {
            this.accepts = regex;
            return this;
        }

        // Rule-INTERNAL-118
        public @NotNull TextInputBuilder browse(final @NotNull Project p, final @NotNull FileChooserDescriptor descriptor) {
            this.browsing = Optional.of(input -> input.browseWith(p, descriptor));
            return this;
        }

        public @NotNull DialogPart<TextInput> build() {
            final @NotNull TextInput input = new TextInput(icon, caption, placeholder, value, accepts);
            browsing.ifPresent(browse -> browse.accept(input));
            return new DialogPart<>(input);
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextFieldBuilder<T> {
        private static final int SEARCH_ROWS = 12;

        private final @NotNull List<Selection<T>> selections = new ArrayList<>();
        private @NotNull Optional<SelectionSource<T>> rows = Optional.empty();
        private @NotNull Icon icon = DialogStyle.NO_ICON;
        private @NotNull String placeholder = "";

        public @NotNull TextFieldBuilder<T> icon(final @NotNull Icon icon) {
            this.icon = icon;
            return this;
        }

        public @NotNull TextFieldBuilder<T> placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextFieldBuilder<T> selection(final @NotNull Icon icon, final @NotNull String name, final @NotNull String hint, final @NotNull T value) {
            selections.add(Selection.of(icon, name, hint, value));
            return this;
        }

        public @NotNull TextFieldBuilder<T> rows(final @NotNull SelectionSource<T> rows) {
            this.rows = Optional.of(rows);
            return this;
        }

        public @NotNull DialogPart<TextFieldWithSelections<T>> build() {
            if (rows.isPresent()) {
                return new DialogPart<>(
                        new TextFieldWithSelections<>(icon, placeholder, List.of(), rows.orElseThrow(), SEARCH_ROWS));
            }

            if (selections.isEmpty()) {
                throw new IllegalStateException(
                        "textFieldWithSelections needs at least one .selection(...) or a .rows(...)");
            }

            final @NotNull List<Selection<T>> fixed = List.copyOf(selections);
            final @NotNull Answer<T> always = Answer.of(fixed);

            return new DialogPart<>(
                    new TextFieldWithSelections<>(icon, placeholder, fixed, _ -> always, fixed.size()));
        }
    }
}
